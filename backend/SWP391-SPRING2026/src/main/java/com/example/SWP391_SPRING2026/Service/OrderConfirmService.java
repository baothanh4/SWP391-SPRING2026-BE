package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.Entity.Order;
import com.example.SWP391_SPRING2026.Entity.Payment;
import com.example.SWP391_SPRING2026.Entity.Shipment;
import com.example.SWP391_SPRING2026.Enum.*;
import com.example.SWP391_SPRING2026.Repository.OrderRepository;
import com.example.SWP391_SPRING2026.Repository.PaymentRepository;
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
    private final PaymentRepository paymentRepository;
    private final PreOrderService preOrderService;

    public void confirmByOperation(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getOrderStatus() != OrderStatus.SUPPORT_CONFIRMED) {
            throw new RuntimeException("Order not approved by support");
        }

        Shipment shipment = order.getShipment();
        if (shipment == null) {
            throw new RuntimeException("Shipment not initialized");
        }

        if (order.getOrderType() == OrderType.PRE_ORDER) {
            preOrderService.validateReadyToShip(order);
            preOrderService.markReadyToShip(order);
        }

        var payments = paymentRepository.findByOrder_Id(order.getId());

        for (Payment p : payments) {
            if (p.getMethod() != PaymentMethod.COD
                    && p.getStatus() != PaymentStatus.SUCCESS) {
                throw new RuntimeException("Online payment not completed");
            }
        }

        String ghnCode = ghnService.createOrder(order);

        shipment.setGhnOrderCode(ghnCode);
        shipment.setStatus(ShipmentStatus.READY_TO_PICK);

        long codAmount = payments.stream()
                .filter(p -> p.getMethod() == PaymentMethod.COD
                        && p.getStatus() == PaymentStatus.UNPAID)
                .mapToLong(Payment::getAmount)
                .sum();

        shipment.setCodAmount(codAmount);
        order.setOrderStatus(OrderStatus.SHIPPING);

        shipmentRepository.save(shipment);
        orderRepository.save(order);
    }

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

        switch (newStatus) {
            case DELIVERED -> {
                shipment.setDeliveredAt(LocalDateTime.now());

                var pays = paymentRepository.findByOrder_Id(order.getId());

                for (Payment p : pays) {
                    if (p.getMethod() == PaymentMethod.COD
                            && p.getStatus() == PaymentStatus.UNPAID) {
                        p.setStatus(PaymentStatus.PAID);
                        p.setPaidAt(LocalDateTime.now());
                    }
                }

                shipment.setCodCollected(true);
                order.setOrderStatus(OrderStatus.COMPLETED);

                if (order.getOrderType() == OrderType.PRE_ORDER) {
                    preOrderService.markFulfilled(order);
                }
            }

            case CANCELLED -> order.setOrderStatus(OrderStatus.CANCELLED);
            case FAILED -> order.setOrderStatus(OrderStatus.FAILED);
        }

        shipmentRepository.save(shipment);
        orderRepository.save(order);
    }

    private ShipmentStatus mapStatus(String ghnStatus) {
        return switch (ghnStatus) {
            case "ready_to_pick" -> ShipmentStatus.READY_TO_PICK;
            case "picking", "money_collect_picking" -> ShipmentStatus.PICKING;
            case "delivering", "money_collect_delivering" -> ShipmentStatus.DELIVERING;
            case "delivered" -> ShipmentStatus.DELIVERED;
            case "delivery_fail" -> ShipmentStatus.FAILED;
            case "return" -> ShipmentStatus.RETURNED;
            case "cancel" -> ShipmentStatus.CANCELLED;
            default -> null;
        };
    }
}