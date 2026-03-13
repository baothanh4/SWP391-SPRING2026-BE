package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Response.OrderResponseDTO;
import com.example.SWP391_SPRING2026.Entity.Order;
import com.example.SWP391_SPRING2026.Entity.Payment;
import com.example.SWP391_SPRING2026.Entity.Shipment;
import com.example.SWP391_SPRING2026.Enum.*;
import com.example.SWP391_SPRING2026.Exception.ResourceNotFoundException;
import com.example.SWP391_SPRING2026.Repository.OrderRepository;
import com.example.SWP391_SPRING2026.Repository.PaymentRepository;
import com.example.SWP391_SPRING2026.Repository.ShipmentRepository;
import com.example.SWP391_SPRING2026.mapper.OrderMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderConfirmService {

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final PaymentRepository paymentRepository;
    private final GhnService ghnService;
    private final PreOrderService preOrderService;
    private final EmailService emailService;

    /*
     SUPPORT CONFIRM ORDER
     */
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

        List<Payment> payments = paymentRepository.findByOrder_Id(order.getId());

        for (Payment p : payments) {
            if (p.getMethod() != PaymentMethod.COD &&
                    p.getStatus() != PaymentStatus.SUCCESS) {
                throw new RuntimeException("Online payment not completed");
            }
        }

        String ghnCode = ghnService.createOrder(order);

        shipment.setGhnOrderCode(ghnCode);
        shipment.setStatus(ShipmentStatus.READY_TO_PICK);

        long codAmount = payments.stream()
                .filter(p -> p.getMethod() == PaymentMethod.COD &&
                        p.getStatus() == PaymentStatus.UNPAID)
                .mapToLong(Payment::getAmount)
                .sum();

        shipment.setCodAmount(codAmount);

        order.setOrderStatus(OrderStatus.SHIPPING);

        shipmentRepository.save(shipment);
        orderRepository.save(order);
    }

    /*
     WEBHOOK UPDATE FROM GHN
     */
    public void updateFromWebhook(String ghnCode, String ghnStatus) {

        Shipment shipment = shipmentRepository
                .findByGhnOrderCode(ghnCode)
                .orElseThrow(() -> new RuntimeException("GHN order not found"));

        ShipmentStatus oldStatus = shipment.getStatus();
        ShipmentStatus newStatus = mapStatus(ghnStatus);

        if (newStatus == null) {
            throw new RuntimeException("Unknown GHN status: " + ghnStatus);
        }

        /*
         Idempotent webhook
         GHN gửi lại cũng không update
         */
        if (oldStatus == newStatus) {
            return;
        }

        shipment.setStatus(newStatus);

        Order order = shipment.getOrder();

        switch (newStatus) {

            case PICKING -> {
                // nothing
            }

            case DELIVERING -> {
                // nothing
            }

            case DELIVERED -> {

                shipment.setDeliveredAt(LocalDateTime.now());

                List<Payment> payments = paymentRepository.findByOrder_Id(order.getId());

                for (Payment p : payments) {

                    if (p.getMethod() == PaymentMethod.COD &&
                            p.getStatus() == PaymentStatus.UNPAID) {

                        p.setStatus(PaymentStatus.PAID);
                        p.setPaidAt(LocalDateTime.now());

                        paymentRepository.save(p);
                    }
                }

                shipment.setCodCollected(true);

                order.setOrderStatus(OrderStatus.COMPLETED);

                if (order.getOrderType() == OrderType.PRE_ORDER) {
                    preOrderService.markFulfilled(order);
                }

                /*
                 SEND EMAIL ONLY ONCE
                 */
                if (oldStatus != ShipmentStatus.DELIVERED) {

                    String email = order.getAddress()
                            .getUser()
                            .getEmail();

                    emailService.sendOrderDeliveredEmail(
                            email,
                            order.getOrderCode()
                    );
                }
            }

            case FAILED -> order.setOrderStatus(OrderStatus.FAILED);

            case CANCELLED -> order.setOrderStatus(OrderStatus.CANCELLED);
        }

        shipmentRepository.save(shipment);
        orderRepository.save(order);
    }

    /*
     MAP GHN STATUS
     */
    private ShipmentStatus mapStatus(String ghnStatus) {

        return switch (ghnStatus) {

            case "ready_to_pick" -> ShipmentStatus.READY_TO_PICK;

            case "picking",
                 "money_collect_picking" -> ShipmentStatus.PICKING;

            case "delivering",
                 "money_collect_delivering" -> ShipmentStatus.DELIVERING;

            case "delivered" -> ShipmentStatus.DELIVERED;

            case "delivery_fail" -> ShipmentStatus.FAILED;

            case "return" -> ShipmentStatus.RETURNED;

            case "cancel" -> ShipmentStatus.CANCELLED;

            default -> null;
        };
    }

    /*
     API SUPPORT
     */

    public List<OrderResponseDTO> getAllOrders() {

        return orderRepository
                .findAll()
                .stream()
                .map(OrderMapper::toResponse)
                .toList();
    }

    public OrderResponseDTO getOrderById(Long orderId) {

        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found"));

        return OrderMapper.toResponse(order);
    }
}