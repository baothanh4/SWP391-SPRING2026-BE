package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.Entity.Order;
import com.example.SWP391_SPRING2026.Entity.Payment;
import com.example.SWP391_SPRING2026.Entity.Shipment;
import com.example.SWP391_SPRING2026.Enum.OrderStatus;
import com.example.SWP391_SPRING2026.Enum.PaymentMethod;
import com.example.SWP391_SPRING2026.Enum.PaymentStatus;
import com.example.SWP391_SPRING2026.Enum.ShipmentStatus;
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

    public void confirmOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getOrderStatus() != OrderStatus.WAITING_CONFIRM) {
            throw new RuntimeException("Invalid order state");
        }

        Shipment shipment = order.getShipment();

        // ===== CALL GHN CREATE ORDER =====
        String ghnCode = ghnService.createOrder(order);

        shipment.setGhnOrderCode(ghnCode);
        shipment.setStatus(ShipmentStatus.READY_TO_PICK);

        if (order.getPayment().getMethod() == PaymentMethod.COD) {
            shipment.setCodAmount(
                    order.getRemainingAmount() != null
                            ? order.getRemainingAmount()
                            : order.getTotalAmount()
            );
        }

        order.setOrderStatus(OrderStatus.SHIPPING);
    }

    @Transactional
    public void updateFromWebhook(String ghnCode, String ghnStatus) {

        Shipment shipment = shipmentRepository
                .findByGhnOrderCode(ghnCode)
                .orElseThrow(()-> new RuntimeException("GHN order code not found"));

        ShipmentStatus newStatus = mapStatus(ghnStatus);
        shipment.setStatus(newStatus);

        Order order = shipment.getOrder();
        Payment payment = order.getPayment();

        switch (newStatus) {

            case DELIVERED -> {
                shipment.setDeliveredAt(LocalDateTime.now());

                if (payment.getMethod() == PaymentMethod.COD) {
                    payment.setStatus(PaymentStatus.PAID);
                    payment.setPaidAt(LocalDateTime.now());
                    shipment.setCodCollected(true);
                }

                order.setOrderStatus(OrderStatus.COMPLETED);
            }

            case CANCELLED ->
                    order.setOrderStatus(OrderStatus.CANCELLED);
        }
    }

    public void confirmByOperation(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow();

        if (order.getOrderStatus() != OrderStatus.SUPPORT_CONFIRMED) {
            throw new RuntimeException("Order not approved by support yet");
        }

        Shipment shipment = order.getShipment();

        // ===== CALL GHN =====
        String ghnCode = ghnService.createOrder(order);

        shipment.setGhnOrderCode(ghnCode);
        shipment.setStatus(ShipmentStatus.READY_TO_PICK);

        order.setOrderStatus(OrderStatus.SHIPPING);
    }

    private ShipmentStatus mapStatus(String ghnStatus) {

        return switch (ghnStatus) {
            case "ready_to_pick" -> ShipmentStatus.READY_TO_PICK;
            case "picking" -> ShipmentStatus.PICKING;
            case "delivering" -> ShipmentStatus.DELIVERING;
            case "delivered" -> ShipmentStatus.DELIVERED;
            case "cancel" -> ShipmentStatus.CANCELLED;
            default -> ShipmentStatus.READY_TO_PICK;
        };
    }
}
