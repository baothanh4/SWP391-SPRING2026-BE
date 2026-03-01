package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.Entity.*;
import com.example.SWP391_SPRING2026.Enum.*;
import com.example.SWP391_SPRING2026.Exception.BadRequestException;
import com.example.SWP391_SPRING2026.Exception.ResourceNotFoundException;
import com.example.SWP391_SPRING2026.Repository.OrderRepository;
import com.example.SWP391_SPRING2026.Repository.ProductVariantRepository;
import com.example.SWP391_SPRING2026.Repository.RefundRequestRepository;
import com.example.SWP391_SPRING2026.Utility.RefundCalculator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderCancellationService {

    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final RefundRequestRepository refundRequestRepository;

    // CUSTOMER cancel: chỉ cho WAITING_CONFIRM (đang đúng rule hiện tại của hệ thống)
    @Transactional
    public void cancelByCustomer(Long userId, Long orderId) {
        Order order = orderRepository.lockById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getAddress().getUser().getId().equals(userId)) {
            throw new BadRequestException("You are not allowed to cancel this order");
        }

        if (order.getOrderStatus() != OrderStatus.WAITING_CONFIRM) {
            throw new BadRequestException("Order cannot be cancelled");
        }

        // 1) update order status
        order.setOrderStatus(OrderStatus.CANCELLED);

        // 2) hoàn stock nếu IN_STOCK
        if (order.getOrderType() == OrderType.IN_STOCK) {
            restockForInStockOrder(order);
        }

        // 3) update shipment
        if (order.getShipment() != null) {
            order.getShipment().setStatus(ShipmentStatus.CANCELLED);
        }

        // 4) payment: KHUNG (nếu chưa paid thì cancel; nếu đã paid thì giữ để trace)
        long paidAmount = resolvePaidAmount(order.getPayment());
        if (order.getPayment() != null && !isPaid(order.getPayment().getStatus())) {
            order.getPayment().setStatus(PaymentStatus.CANCELLED);
        }

        // 5) PRE_ORDER: tạo refund request theo policy “mất cọc” (record thôi)
        if (order.getOrderType() == OrderType.PRE_ORDER) {
            long deposit = order.getDeposit() == null ? 0L : order.getDeposit();
            long refundAmount = RefundCalculator.calculate(paidAmount, deposit, RefundPolicy.DEPOSIT_FORFEIT);

            if (refundAmount > 0) {
                createRefundRequest(order, RefundReason.CUSTOMER_CANCEL, RefundPolicy.DEPOSIT_FORFEIT,
                        refundAmount, userId, "CUSTOMER", "Auto from customer cancel");
            }
        }
    }

    // STAFF cancel: tạo refund request FULL_REFUND (record thôi)
    @Transactional
    public void cancelByStaff(Long staffId, String role, Long orderId, RefundReason reason, String note) {
        Order order = orderRepository.lockById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getOrderStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("Completed order cannot be cancelled");
        }

        order.setOrderStatus(OrderStatus.CANCELLED);

        if (order.getOrderType() == OrderType.IN_STOCK) {
            restockForInStockOrder(order);
        }

        if (order.getShipment() != null) {
            order.getShipment().setStatus(ShipmentStatus.CANCELLED);
        }

        long paidAmount = resolvePaidAmount(order.getPayment());
        if (order.getPayment() != null && !isPaid(order.getPayment().getStatus())) {
            order.getPayment().setStatus(PaymentStatus.CANCELLED);
        }

        if (paidAmount > 0) {
            RefundReason safeReason = (reason == null) ? RefundReason.SHOP_CANNOT_SUPPLY : reason;
            createRefundRequest(order, safeReason, RefundPolicy.FULL_REFUND,
                    paidAmount, staffId, role, note == null ? "Staff cancel" : note);
        }
    }

    private void restockForInStockOrder(Order order) {
        for (OrderItems item : order.getOrderItems()) {
            // nếu item là combo => hoàn từng variant trong combo
            if (Boolean.TRUE.equals(item.getIsCombo()) && item.getProductCombo() != null) {
                ProductCombo combo = item.getProductCombo();
                int orderQty = item.getQuantity() == null ? 0 : item.getQuantity();

                for (ComboItem comboItem : combo.getItems()) {
                    ProductVariant v = productVariantRepository
                            .lockById(comboItem.getProductVariant().getId())
                            .orElseThrow(() -> new BadRequestException("Variant not found"));

                    int required = comboItem.getQuantity() * orderQty;
                    v.setStockQuantity(v.getStockQuantity() + required);
                }
            } else {
                // item thường
                ProductVariant v = item.getProductVariant();
                if (v != null) {
                    int qty = item.getQuantity() == null ? 0 : item.getQuantity();
                    ProductVariant locked = productVariantRepository
                            .lockById(v.getId())
                            .orElseThrow(() -> new BadRequestException("Variant not found"));

                    locked.setStockQuantity(locked.getStockQuantity() + qty);
                }
            }
        }
    }

    private void createRefundRequest(Order order,
                                     RefundReason reason,
                                     RefundPolicy policy,
                                     long refundAmount,
                                     Long actorUserId,
                                     String actorRole,
                                     String note) {

        RefundRequest rr = new RefundRequest();
        rr.setOrder(order);
        rr.setReason(reason);
        rr.setPolicy(policy);
        rr.setStatus(RefundRequestStatus.REQUESTED);
        rr.setRefundAmount(refundAmount);
        rr.setCreatedByUserId(actorUserId);
        rr.setCreatedByRole(actorRole);
        rr.setNote(note);
        rr.setCreatedAt(LocalDateTime.now());
        rr.setUpdatedAt(LocalDateTime.now());

        refundRequestRepository.save(rr);

        // TODO: integrate VNPay refund later
    }

    private long resolvePaidAmount(Payment payment) {
        if (payment == null) return 0L;
        if (isPaid(payment.getStatus())) {
            return payment.getAmount() == null ? 0L : payment.getAmount();
        }
        return 0L;
    }

    private boolean isPaid(PaymentStatus st) {
        return st == PaymentStatus.SUCCESS || st == PaymentStatus.PAID;
    }
}