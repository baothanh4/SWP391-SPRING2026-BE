package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Response.ConfirmResponseOrderDTO;
import com.example.SWP391_SPRING2026.DTO.Response.OrderResponseDTO;
import com.example.SWP391_SPRING2026.Entity.*;
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
        OPERATION CONFIRM ORDER
     */
    @Transactional
    public ConfirmResponseOrderDTO confirmByOperation(Long orderId) {

        Order order = orderRepository.lockById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        reconcileOperationalState(order);

        if (!canProceedToOperationShipment(order)) {
            throw new RuntimeException("Order not approved by support");
        }

        Shipment shipment = order.getShipment();
        if (shipment == null) {
            throw new RuntimeException("Shipment not initialized");
        }

    /*
        PREORDER VALIDATION
     */
        if (order.getOrderType() == OrderType.PRE_ORDER) {
            preOrderService.validateReadyToShip(order);
        }

    /*
        PAYMENT VALIDATION
     */
        List<Payment> payments = paymentRepository.findByOrder_Id(order.getId());

        for (Payment p : payments) {
            if (p.getMethod() != PaymentMethod.COD &&
                    p.getStatus() != PaymentStatus.SUCCESS) {
                throw new RuntimeException("Online payment not completed");
            }
        }

    /*
        CALCULATE COD
     */
        long codAmount = payments.stream()
                .filter(p ->
                        p.getMethod() == PaymentMethod.COD &&
                                p.getStatus() == PaymentStatus.UNPAID)
                .mapToLong(Payment::getAmount)
                .sum();

    /*
        CREATE GHN ORDER
     */
        String ghnCode = ghnService.createOrder(order);

        shipment.setGhnOrderCode(ghnCode);
        shipment.setStatus(ShipmentStatus.READY_TO_PICK);
        shipment.setCodAmount(codAmount);

        if (order.getOrderType() == OrderType.PRE_ORDER) {
            preOrderService.markReadyToShip(order);
        }

        order.setApprovalStatus(ApprovalStatus.OPERATION_CONFIRMED);
        order.setOperationConfirmedAt(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.SHIPPING);

        shipmentRepository.save(shipment);
        orderRepository.save(order);

        return ConfirmResponseOrderDTO.builder()
                .orderCode(order.getOrderCode())
                .ghnOrderCode(ghnCode)
                .shipmentStatus(shipment.getStatus())
                .orderStatus(order.getOrderStatus())
                .approvalStatus(order.getApprovalStatus())
                .supportApprovedAt(order.getSupportApprovedAt())
                .operationConfirmedAt(order.getOperationConfirmedAt())
                .build();
    }

    /*
        GHN WEBHOOK UPDATE
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
            IDEMPOTENT
         */
        if (oldStatus == newStatus) {
            return;
        }

        shipment.setStatus(newStatus);

        Order order = shipment.getOrder();

        switch (newStatus) {

            case PICKING -> {}

            case PICKED -> {}

            case DELIVERING -> {

            }

            case DELIVERED -> {

                shipment.setDeliveredAt(LocalDateTime.now());

                shipment.setShippedAt(LocalDateTime.now());

                List<Payment> payments = paymentRepository.findByOrder_Id(order.getId());

                for (Payment p : payments) {

                    if (p.getMethod() == PaymentMethod.COD &&
                            p.getStatus() == PaymentStatus.UNPAID) {

                        p.setStatus(PaymentStatus.SUCCESS);

                        p.setPaidAt(LocalDateTime.now());

                        paymentRepository.save(p);
                    }
                }

                shipment.setCodCollected(true);

                if (order.getOrderType() == OrderType.PRE_ORDER) {
                    preOrderService.markFulfilled(order);
                } else {
                    order.setOrderStatus(OrderStatus.COMPLETED);
                }

                if (oldStatus != ShipmentStatus.DELIVERED) {

                    String email = order.getUser().getEmail();

                    emailService.sendOrderDeliveredEmail(
                            email,
                            order.getOrderCode()
                    );
                }
            }

            case FAILED -> order.setOrderStatus(OrderStatus.FAILED);

            case CANCELLED -> order.setOrderStatus(OrderStatus.CANCELLED);

            case RETURNED -> order.setOrderStatus(OrderStatus.CANCELLED);
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

            case "picked"  -> ShipmentStatus.PICKED;

            case "cancel" -> ShipmentStatus.CANCELLED;

            default -> null;
        };
    }

    public OrderResponseDTO getOrderByGhnCode(String ghnCode) {
        Shipment shipment = shipmentRepository.findByGhnOrderCode(ghnCode).orElseThrow(() -> new RuntimeException("GHN order not found"));

        Order order = shipment.getOrder();

        return OrderMapper.toResponse(order);
    }

    public List<OrderResponseDTO> getAllOrders() {

        return orderRepository
                .findAll()
                .stream()
                .map(this::reconcileOperationalState)
                .map(OrderMapper::toResponse)
                .toList();
    }

    public OrderResponseDTO getOrderById(Long orderId) {

        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found"));

        reconcileOperationalState(order);

        return OrderMapper.toResponse(order);
    }

    public ShipmentStatus mapStatusExternal(String ghnStatus) {
        return mapStatus(ghnStatus);
    }

    public boolean isSupportApprovedForOperations(Order order) {
        return order.getApprovalStatus() == ApprovalStatus.SUPPORT_APPROVED
                || order.getApprovalStatus() == ApprovalStatus.OPERATION_CONFIRMED
                || order.getSupportApprovedAt() != null;
    }

    private boolean hasCompletedPayment(Order order) {
        List<Payment> payments = paymentRepository.findByOrder_Id(order.getId());
        if (payments == null || payments.isEmpty()) {
            return false;
        }

        return payments.stream().allMatch(payment ->
                payment.getMethod() == PaymentMethod.COD
                        || payment.getStatus() == PaymentStatus.SUCCESS
                        || payment.getStatus() == PaymentStatus.PAID
        );
    }

    public boolean canProceedToOperationShipment(Order order) {
        if (isSupportApprovedForOperations(order)) {
            return true;
        }

        return order.getOrderType() == OrderType.IN_STOCK
                && hasCompletedPayment(order)
                && order.getOrderStatus() != OrderStatus.SHIPPING
                && order.getOrderStatus() != OrderStatus.COMPLETED
                && order.getOrderStatus() != OrderStatus.CANCELLED
                && order.getOrderStatus() != OrderStatus.FAILED;
    }

    public Order reconcileOperationalState(Order order) {
        if (order == null) {
            return null;
        }

        boolean changed = false;

        Shipment shipment = order.getShipment();
        boolean hasGhnOrder = shipment != null && shipment.getGhnOrderCode() != null && !shipment.getGhnOrderCode().isBlank();
        ShipmentStatus shipmentStatus = shipment != null ? shipment.getStatus() : null;

        if (order.getOrderType() == OrderType.IN_STOCK) {
            boolean supportApproved = isSupportApprovedForOperations(order);
            boolean paymentCompleted = hasCompletedPayment(order);

            if (order.getApprovalStatus() == ApprovalStatus.PENDING_SUPPORT && order.getSupportApprovedAt() != null) {
                order.setApprovalStatus(ApprovalStatus.SUPPORT_APPROVED);
                supportApproved = true;
                changed = true;
            }

            if (!supportApproved && paymentCompleted) {
                supportApproved = true;
            }

            if (supportApproved
                    && !hasGhnOrder
                    && shipmentStatus != ShipmentStatus.READY_TO_PICK
                    && shipmentStatus != ShipmentStatus.PICKING
                    && shipmentStatus != ShipmentStatus.PICKED
                    && shipmentStatus != ShipmentStatus.DELIVERING
                    && shipmentStatus != ShipmentStatus.DELIVERED
                    && shipmentStatus != ShipmentStatus.FAILED
                    && shipmentStatus != ShipmentStatus.CANCELLED
                    && shipmentStatus != ShipmentStatus.RETURNED
                    && order.getOrderStatus() != OrderStatus.SUPPORT_CONFIRMED) {
                order.setOrderStatus(OrderStatus.SUPPORT_CONFIRMED);
                changed = true;
            }
        }

        if ((order.getApprovalStatus() == ApprovalStatus.OPERATION_CONFIRMED || order.getOperationConfirmedAt() != null)
                && order.getOrderStatus() != OrderStatus.SHIPPING
                && order.getOrderStatus() != OrderStatus.COMPLETED
                && order.getOrderStatus() != OrderStatus.CANCELLED
                && order.getOrderStatus() != OrderStatus.FAILED) {
            order.setOrderStatus(OrderStatus.SHIPPING);
            changed = true;
        }

        if (changed) {
            return orderRepository.save(order);
        }

        return order;
    }
}
