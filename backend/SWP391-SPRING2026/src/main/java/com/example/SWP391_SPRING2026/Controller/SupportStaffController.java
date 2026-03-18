package com.example.SWP391_SPRING2026.Controller;


import com.example.SWP391_SPRING2026.DTO.Request.CancelOrderByStaffRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Response.OrderResponseDTO;
import com.example.SWP391_SPRING2026.Entity.Order;
import com.example.SWP391_SPRING2026.Entity.Payment;
import com.example.SWP391_SPRING2026.Entity.UserPrincipal;
import com.example.SWP391_SPRING2026.Enum.OrderStatus;
import com.example.SWP391_SPRING2026.Enum.PaymentMethod;
import com.example.SWP391_SPRING2026.Enum.PaymentStatus;
import com.example.SWP391_SPRING2026.Enum.RefundReason;
import com.example.SWP391_SPRING2026.Repository.OrderRepository;
import com.example.SWP391_SPRING2026.Repository.PaymentRepository;
import com.example.SWP391_SPRING2026.Service.OrderCancellationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support_staff/orders")
@RequiredArgsConstructor
public class SupportStaffController {

    private final OrderRepository orderRepository;
    private final OrderCancellationService orderCancellationService;
    private final PaymentRepository paymentRepository;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<OrderResponseDTO> getAllOrders() {
        return orderCancellationService.getAllOrders();
    }

    @GetMapping("/{orderId}")
    public OrderResponseDTO getOrderById(@PathVariable Long orderId) {
        return orderCancellationService.getOrderById(orderId);
    }

    // 2️⃣ Confirm đơn (Support duyệt)
    @PostMapping("/{orderId}/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<String> confirmOrder(
            @PathVariable Long orderId) {

        Order order = orderRepository.lockById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getOrderStatus().canBeConfirmedBySupport()) {
            throw new RuntimeException("Invalid order state");
        }

        // ✅ validate payment
        List<Payment> payments = paymentRepository.findByOrder_Id(orderId);

        boolean valid = payments.stream().allMatch(p ->
                p.getMethod() == PaymentMethod.COD ||
                        p.getStatus() == PaymentStatus.SUCCESS
        );

        if (!valid) {
            throw new RuntimeException("Payment not completed");
        }

        order.setOrderStatus(OrderStatus.SUPPORT_CONFIRMED);

        return ResponseEntity.ok("Order confirmed by support");
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
