package com.example.SWP391_SPRING2026.Controller;

import com.example.SWP391_SPRING2026.DTO.Response.OrderResponseDTO;
import com.example.SWP391_SPRING2026.Entity.Order;
import com.example.SWP391_SPRING2026.Enum.OrderStatus;
import com.example.SWP391_SPRING2026.Repository.OrderRepository;
import com.example.SWP391_SPRING2026.Service.OrderConfirmService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

    @GetMapping("/approved")
    @ResponseStatus(HttpStatus.OK)
    public Page<Order> getSupportApproved(Pageable pageable) {
        return orderRepository.findByOrderStatus(
                OrderStatus.SUPPORT_CONFIRMED,
                pageable
        );
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
    public ResponseEntity<String> confirmByOperation(
            @PathVariable Long orderId) {

        orderConfirmService.confirmByOperation(orderId);

        return ResponseEntity.ok("Order sent to GHN");
    }
}
