package com.example.SWP391_SPRING2026.Controller;

import com.example.SWP391_SPRING2026.DTO.Response.ConfirmResponseOrderDTO;
import com.example.SWP391_SPRING2026.DTO.Response.OrderResponseDTO;
import com.example.SWP391_SPRING2026.Entity.Order;
import com.example.SWP391_SPRING2026.Enum.ApprovalStatus;
import com.example.SWP391_SPRING2026.Enum.OrderStatus;
import com.example.SWP391_SPRING2026.Enum.OrderType;
import com.example.SWP391_SPRING2026.Repository.OrderRepository;
import com.example.SWP391_SPRING2026.Service.OrderConfirmService;
import com.example.SWP391_SPRING2026.Service.PreOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operation_staff/orders")
@RequiredArgsConstructor
public class OperationalOrderController {

    private final OrderRepository orderRepository;
    private final OrderConfirmService orderConfirmService;
    private final PreOrderService preOrderService;

    @GetMapping("/approved")
    @ResponseStatus(HttpStatus.OK)
    public Page<Order> getSupportApproved(Pageable pageable) {
        Page<Order> page = orderRepository.findOperationApprovedOrders(
                ApprovalStatus.SUPPORT_APPROVED,
                List.of(
                        OrderStatus.SHIPPING,
                        OrderStatus.COMPLETED,
                        OrderStatus.CANCELLED,
                        OrderStatus.FAILED
                ),
                pageable
        );

        List<Order> filtered = page.getContent().stream()
                .map(orderConfirmService::reconcileOperationalState)
                .filter(order -> {
                    if (order.getOrderType() != OrderType.PRE_ORDER) {
                        return orderConfirmService.canProceedToOperationShipment(order);
                    }
                    return preOrderService.isReadyForOperation(order);
                })
                .toList();

        return new PageImpl<>(filtered, pageable, filtered.size());
    }
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<OrderResponseDTO> getAllOrders() {
        return orderConfirmService.getAllOrders();
    }

    @GetMapping("/{orderId}")
    public OrderResponseDTO getOrderById(@PathVariable Long orderId) {
        return orderConfirmService.getOrderById(orderId);
    }

    @PostMapping("/{orderId}/confirm")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ConfirmResponseOrderDTO> confirmByOperation(
            @PathVariable Long orderId) {

        return ResponseEntity.ok(orderConfirmService.confirmByOperation(orderId));
    }
}
