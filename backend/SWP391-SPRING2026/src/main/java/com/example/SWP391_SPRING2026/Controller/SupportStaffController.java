package com.example.SWP391_SPRING2026.Controller;

import com.example.SWP391_SPRING2026.DTO.Request.CancelOrderByStaffRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Response.ConfirmResponseOrderDTO;
import com.example.SWP391_SPRING2026.DTO.Response.OrderResponseDTO;
import com.example.SWP391_SPRING2026.Entity.Order;
import com.example.SWP391_SPRING2026.Entity.Payment;
import com.example.SWP391_SPRING2026.Entity.Shipment;
import com.example.SWP391_SPRING2026.Entity.UserPrincipal;
import com.example.SWP391_SPRING2026.Enum.*;
import com.example.SWP391_SPRING2026.Repository.OrderRepository;
import com.example.SWP391_SPRING2026.Repository.PaymentRepository;
import com.example.SWP391_SPRING2026.Service.OrderCancellationService;
import com.example.SWP391_SPRING2026.Service.PreOrderService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/support_staff/orders")
@RequiredArgsConstructor
public class SupportStaffController {

    private final OrderRepository orderRepository;
    private final OrderCancellationService orderCancellationService;
    private final PaymentRepository paymentRepository;
    private final PreOrderService preOrderService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<OrderResponseDTO> getAllOrders() {
        return orderCancellationService.getAllOrders();
    }

    @GetMapping("/{orderId}")
    public OrderResponseDTO getOrderById(@PathVariable Long orderId) {
        return orderCancellationService.getOrderById(orderId);
    }

    @PostMapping("/{orderId}/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ResponseEntity<ConfirmResponseOrderDTO> confirmOrder(@PathVariable Long orderId) {

        Order order = orderRepository.lockById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getOrderStatus() != OrderStatus.WAITING_CONFIRM
                && order.getOrderStatus() != OrderStatus.PAID
                && order.getOrderStatus() != OrderStatus.PENDING_PAYMENT
                && order.getOrderStatus() != OrderStatus.CONFIRMED) {
            throw new RuntimeException("Order status is not valid for support confirm");
        }

        List<Payment> payments = paymentRepository.findByOrder_Id(orderId);

        boolean valid = payments.stream().allMatch(p ->
                p.getMethod() == PaymentMethod.COD ||
                        p.getStatus() == PaymentStatus.SUCCESS
        );

        if (!valid) {
            throw new RuntimeException("Payment not completed");
        }

        order.setApprovalStatus(ApprovalStatus.SUPPORT_APPROVED);
        order.setSupportApprovedAt(LocalDateTime.now());

        if (order.getOrderType() == OrderType.IN_STOCK) {
            order.setOrderStatus(OrderStatus.SUPPORT_CONFIRMED);
        } else if (order.getOrderType() == OrderType.PRE_ORDER) {
            if (preOrderService.isReadyForOperation(order)) {
                order.setOrderStatus(OrderStatus.SUPPORT_CONFIRMED);
            }
        }

        orderRepository.save(order);

        Shipment shipment = order.getShipment();

        ConfirmResponseOrderDTO response = ConfirmResponseOrderDTO.builder()
                .orderCode(order.getOrderCode())
                .ghnOrderCode(shipment != null ? shipment.getGhnOrderCode() : null)
                .shipmentStatus(shipment != null ? shipment.getStatus() : null)
                .orderStatus(order.getOrderStatus())
                .approvalStatus(order.getApprovalStatus())
                .supportApprovedAt(order.getSupportApprovedAt())
                .operationConfirmedAt(order.getOperationConfirmedAt())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{orderId}/cancel")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<String> cancelOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long orderId,
            @RequestBody(required = false) CancelOrderByStaffRequestDTO body
    ) {
        RefundReason reason = (body == null || body.getReason() == null)
                ? RefundReason.SHOP_CANNOT_SUPPLY
                : body.getReason();

        String note = (body == null) ? null : body.getNote();

        orderCancellationService.cancelByStaff(
                principal.getUserId(),
                principal.getRole(),
                orderId,
                reason,
                note
        );

        return ResponseEntity.ok("Order cancelled by support");
    }
}