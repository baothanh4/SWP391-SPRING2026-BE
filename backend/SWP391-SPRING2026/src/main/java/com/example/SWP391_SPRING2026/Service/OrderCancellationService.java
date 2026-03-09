package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.Entity.*;
import com.example.SWP391_SPRING2026.Enum.*;
import com.example.SWP391_SPRING2026.Exception.BadRequestException;
import com.example.SWP391_SPRING2026.Exception.ResourceNotFoundException;
import com.example.SWP391_SPRING2026.Repository.*;
import com.example.SWP391_SPRING2026.Utility.RefundCalculator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderCancellationService {

    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final PaymentRepository paymentRepository;

    // =========================================================
    // CUSTOMER CANCEL
    // =========================================================
    @Transactional
    public void cancelByCustomer(Long userId, Long orderId) {

        Order order = orderRepository.lockById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Check ownership
        if (order.getAddress() == null
                || order.getAddress().getUser() == null
                || !order.getAddress().getUser().getId().equals(userId)) {

            throw new BadRequestException("You are not allowed to cancel this order");
        }

        if (order.getOrderStatus() != OrderStatus.WAITING_CONFIRM) {
            throw new BadRequestException("Order cannot be cancelled");
        }

        order.setOrderStatus(OrderStatus.CANCELLED);

        if (order.getOrderType() == OrderType.IN_STOCK) {
            restockForInStockOrder(order);
        }

        if (order.getShipment() != null) {
            order.getShipment().setStatus(ShipmentStatus.CANCELLED);
        }

        long paidAmount = handlePayments(order.getId());

        // PRE_ORDER → mất cọc
        if (order.getOrderType() == OrderType.PRE_ORDER) {

            long deposit = order.getDeposit() == null ? 0L : order.getDeposit();

            long refundAmount = RefundCalculator.calculate(
                    paidAmount,
                    deposit,
                    RefundPolicy.DEPOSIT_FORFEIT
            );

            if (refundAmount > 0) {
                createRefundRequest(
                        order,
                        RefundReason.CUSTOMER_CANCEL,
                        RefundPolicy.DEPOSIT_FORFEIT,
                        refundAmount,
                        userId,
                        "CUSTOMER",
                        "Auto from customer cancel"
                );
            }
        }
    }

    // =========================================================
    // STAFF CANCEL
    // =========================================================
    @Transactional
    public void cancelByStaff(Long staffId,
                              String role,
                              Long orderId,
                              RefundReason reason,
                              String note) {

        if (!"SUPPORT_STAFF".equals(role)) {
            throw new BadRequestException("Access denied");
        }

        Order order = orderRepository.lockById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order already cancelled");
        }

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

        long paidAmount = handlePayments(order.getId());

        if (paidAmount > 0) {

            if (refundRequestRepository.existsByOrderIdAndStatus(
                    order.getId(),
                    RefundRequestStatus.REQUESTED)) {

                throw new BadRequestException("Refund already requested");
            }

            createRefundRequest(
                    order,
                    reason == null ? RefundReason.SHOP_CANNOT_SUPPLY : reason,
                    RefundPolicy.FULL_REFUND,
                    paidAmount,
                    staffId,
                    role,
                    note
            );
        }
    }

    // =========================================================
    // MULTI PAYMENT HANDLER (1–N SAFE)
    // =========================================================
    private long handlePayments(Long orderId) {

        List<Payment> payments = paymentRepository.findByOrder_Id(orderId);

        long paidAmount = 0L;

        for (Payment p : payments) {

            if (isPaid(p.getStatus())) {
                paidAmount += p.getAmount() == null ? 0L : p.getAmount();
            } else {
                p.setStatus(PaymentStatus.CANCELLED);
            }
        }

        return paidAmount;
    }

    // =========================================================
    // RESTOCK
    // =========================================================
    private void restockForInStockOrder(Order order) {

        for (OrderItems item : order.getOrderItems()) {

            if (Boolean.TRUE.equals(item.getIsCombo())
                    && item.getProductCombo() != null) {

                ProductCombo combo = item.getProductCombo();
                int orderQty = item.getQuantity() == null ? 0 : item.getQuantity();

                for (ComboItem comboItem : combo.getItems()) {

                    ProductVariant variant = productVariantRepository
                            .lockById(comboItem.getProductVariant().getId())
                            .orElseThrow(() -> new BadRequestException("Variant not found"));

                    int required = comboItem.getQuantity() * orderQty;
                    variant.setStockQuantity(variant.getStockQuantity() + required);
                }

            } else {

                ProductVariant variant = item.getProductVariant();

                if (variant != null) {

                    int qty = item.getQuantity() == null ? 0 : item.getQuantity();

                    ProductVariant locked = productVariantRepository
                            .lockById(variant.getId())
                            .orElseThrow(() -> new BadRequestException("Variant not found"));

                    locked.setStockQuantity(locked.getStockQuantity() + qty);
                }
            }
        }
    }

    // =========================================================
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
    }

    private boolean isPaid(PaymentStatus status) {
        return status == PaymentStatus.SUCCESS
                || status == PaymentStatus.PAID;
    }
}