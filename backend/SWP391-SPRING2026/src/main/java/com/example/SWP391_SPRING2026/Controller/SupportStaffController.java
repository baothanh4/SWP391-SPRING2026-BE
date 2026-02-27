package com.example.SWP391_SPRING2026.Controller;


import com.example.SWP391_SPRING2026.DTO.Request.CancelOrderByStaffRequestDTO;
import com.example.SWP391_SPRING2026.Entity.Order;
import com.example.SWP391_SPRING2026.Entity.UserPrincipal;
import com.example.SWP391_SPRING2026.Enum.OrderStatus;
import com.example.SWP391_SPRING2026.Enum.RefundReason;
import com.example.SWP391_SPRING2026.Repository.OrderRepository;
import com.example.SWP391_SPRING2026.Service.OrderCancellationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/support_staff/orders")
@RequiredArgsConstructor
public class SupportStaffController {

    private final OrderRepository orderRepository;
    private final OrderCancellationService orderCancellationService;

    // 1️⃣ Danh sách đơn chờ support duyệt
    @GetMapping("/waiting")
    @ResponseStatus(HttpStatus.OK)
    public Page<Order> getWaitingOrders(Pageable pageable) {
        return orderRepository.findByOrderStatus(
                OrderStatus.WAITING_CONFIRM,
                pageable
        );
    }

    // 2️⃣ Confirm đơn (Support duyệt)
    @PostMapping("/{orderId}/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<String> confirmOrder(
            @PathVariable Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getOrderStatus() != OrderStatus.WAITING_CONFIRM) {
            throw new RuntimeException("Order is not waiting for support confirm");
        }

        order.setOrderStatus(OrderStatus.SUPPORT_CONFIRMED);

        orderRepository.save(order);

        return ResponseEntity.ok("Order confirmed by support");
    }

    // 3️⃣ Support hủy đơn
    @PostMapping("/{orderId}/cancel")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<String> cancelOrder(
            @PathVariable Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow();

        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        return ResponseEntity.ok("Order cancelled by support");
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
