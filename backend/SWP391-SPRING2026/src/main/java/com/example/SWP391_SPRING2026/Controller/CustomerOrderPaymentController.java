package com.example.SWP391_SPRING2026.Controller;

import com.example.SWP391_SPRING2026.DTO.Response.PayRemainingResponseDTO;
import com.example.SWP391_SPRING2026.Entity.Order;
import com.example.SWP391_SPRING2026.Entity.OrderPayment;
import com.example.SWP391_SPRING2026.Entity.UserPrincipal;
import com.example.SWP391_SPRING2026.Enum.*;
import com.example.SWP391_SPRING2026.Exception.BadRequestException;
import com.example.SWP391_SPRING2026.Repository.OrderPaymentRepository;
import com.example.SWP391_SPRING2026.Repository.OrderRepository;
import com.example.SWP391_SPRING2026.Service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/customer/orders")
@RequiredArgsConstructor
public class CustomerOrderPaymentController {

    private final OrderRepository orderRepository;
    private final OrderPaymentRepository orderPaymentRepository;
    private final VNPayService vnPayService;

    @PostMapping("/{orderId}/pay-remaining")
    @Transactional
    public PayRemainingResponseDTO payRemaining(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long orderId,
            HttpServletRequest request
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getAddress().getUser().getId().equals(principal.getUserId())) {
            throw new BadRequestException("You are not allowed to pay this order");
        }

        if (order.getOrderType() != OrderType.PRE_ORDER) {
            throw new BadRequestException("Only PRE_ORDER supports remaining payment");
        }

        long remaining = order.getRemainingAmount() == null ? 0L : order.getRemainingAmount();
        if (remaining <= 0) {
            throw new BadRequestException("No remaining amount to pay");
        }

        // Chỉ cho pay remaining trước khi operation tạo GHN (tránh đổi phương thức khi đã tạo COD)
        if (order.getOrderStatus() == OrderStatus.SHIPPING || order.getOrderStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("Cannot pay remaining after shipping started");
        }

        // Nếu đã có REMAINING SUCCESS rồi thì thôi
        var existingRemaining = orderPaymentRepository
                .findTopByOrder_IdAndStageOrderByIdDesc(orderId, PaymentStage.REMAINING)
                .orElse(null);

        if (existingRemaining != null && existingRemaining.getMethod() == PaymentMethod.VNPAY
                && existingRemaining.getStatus() == PaymentStatus.SUCCESS) {
            throw new BadRequestException("Remaining already paid");
        }

        // Nếu trước đó đang chọn COD remaining và đã tạo REMAINING COD UNPAID, thì cancel nó để chuyển sang VNPAY
        if (existingRemaining != null
                && existingRemaining.getMethod() == PaymentMethod.COD
                && existingRemaining.getStatus() == PaymentStatus.UNPAID) {

            existingRemaining.setStatus(PaymentStatus.CANCELLED);
        }

        order.setRemainingPaymentMethod(PaymentMethod.VNPAY);

        OrderPayment pay = new OrderPayment();
        pay.setOrder(order);
        pay.setStage(PaymentStage.REMAINING);
        pay.setMethod(PaymentMethod.VNPAY);
        pay.setAmount(remaining);
        pay.setStatus(PaymentStatus.PENDING);
        pay.setCreatedAt(LocalDateTime.now());

        orderPaymentRepository.save(pay);

        String ipAddress = request.getRemoteAddr();
        if (ipAddress == null || ipAddress.equals("0:0:0:0:0:0:0:1")) {
            ipAddress = "127.0.0.1";
        }

        String paymentUrl;
        try {
            paymentUrl = vnPayService.createVNPayUrl(pay.getId().toString(), remaining, ipAddress);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create VNPay URL");
        }

        return new PayRemainingResponseDTO(orderId, pay.getId(), remaining, paymentUrl);
    }
}