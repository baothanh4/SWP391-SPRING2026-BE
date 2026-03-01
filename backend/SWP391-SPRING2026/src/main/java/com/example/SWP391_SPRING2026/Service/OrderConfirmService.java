package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.Entity.Order;
import com.example.SWP391_SPRING2026.Entity.Payment;
import com.example.SWP391_SPRING2026.Entity.Shipment;
import com.example.SWP391_SPRING2026.Enum.*;
import com.example.SWP391_SPRING2026.Repository.OrderPaymentRepository;
import com.example.SWP391_SPRING2026.Repository.OrderRepository;
import com.example.SWP391_SPRING2026.Repository.ShipmentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderConfirmService {

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final GhnService ghnService;
    private final OrderPaymentRepository orderPaymentRepository;

    // =========================================================
    // OPERATION CONFIRM ORDER → CREATE GHN
    // =========================================================
    public void confirmByOperation(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getOrderStatus() != OrderStatus.SUPPORT_CONFIRMED) {
            throw new RuntimeException("Order not approved by support");
        }

        Payment payment = order.getPayment();
        if (payment == null) {
            throw new RuntimeException("Payment not found");
        }

        Shipment shipment = order.getShipment();
        if (shipment == null) {
            throw new RuntimeException("Shipment not initialized");
        }

        // 🔥 CHECK PAYMENT FOR ONLINE METHODS
        if (payment.getMethod() != PaymentMethod.COD &&
                payment.getStatus() != PaymentStatus.SUCCESS) {

            throw new RuntimeException("Online payment not completed");
        }

        // ===== CALL GHN =====
        String ghnCode = ghnService.createOrder(order);

        shipment.setGhnOrderCode(ghnCode);
        shipment.setStatus(ShipmentStatus.READY_TO_PICK);

        // COD amount
        if (payment.getMethod() == PaymentMethod.COD) {
            shipment.setCodAmount(
                    order.getRemainingAmount() != null
                            ? order.getRemainingAmount()
                            : order.getTotalAmount()
            );
        }

        order.setOrderStatus(OrderStatus.SHIPPING);

        shipmentRepository.save(shipment);
        orderRepository.save(order);
    }

    // =========================================================
    // GHN WEBHOOK UPDATE
    // =========================================================
    public void updateFromWebhook(String ghnCode, String ghnStatus) {

        Shipment shipment = shipmentRepository
                .findByGhnOrderCode(ghnCode)
                .orElseThrow(() -> new RuntimeException("GHN order not found"));

        ShipmentStatus newStatus = mapStatus(ghnStatus);

        if (newStatus == null) {
            throw new RuntimeException("Unknown GHN status: " + ghnStatus);
        }

        shipment.setStatus(newStatus);

        Order order = shipment.getOrder();
        Payment payment = order.getPayment();

        switch (newStatus) {

            case DELIVERED -> {
                shipment.setDeliveredAt(LocalDateTime.now());

                // mark all COD unpaid payments as PAID
                var pays = orderPaymentRepository.findByOrder_Id(order.getId());
                for (var p : pays) {
                    if (p.getMethod() == PaymentMethod.COD && p.getStatus() == PaymentStatus.UNPAID) {
                        p.setStatus(PaymentStatus.PAID);
                        p.setPaidAt(LocalDateTime.now());
                    }
                }
                shipment.setCodCollected(true);

                order.setOrderStatus(OrderStatus.COMPLETED);
            }

            case CANCELLED -> {
                order.setOrderStatus(OrderStatus.CANCELLED);
            }

            case FAILED -> {
                order.setOrderStatus(OrderStatus.FAILED);
            }

            default -> {
                // Không đổi order status
            }
        }

        shipmentRepository.save(shipment);
        orderRepository.save(order);
    }

    // =========================================================
    // MAP GHN STATUS
    // =========================================================
    private ShipmentStatus mapStatus(String ghnStatus) {

        return switch (ghnStatus) {

            case "ready_to_pick" -> ShipmentStatus.READY_TO_PICK;
            case "picking", "money_collect_picking" -> ShipmentStatus.PICKING;
            case "delivering", "money_collect_delivering" -> ShipmentStatus.DELIVERING;
            case "delivered" -> ShipmentStatus.DELIVERED;
            case "delivery_fail" -> ShipmentStatus.FAILED;
            case "return" -> ShipmentStatus.RETURNED;
            case "cancel" -> ShipmentStatus.CANCELLED;

            default -> null; // safer
        };
    }
}
