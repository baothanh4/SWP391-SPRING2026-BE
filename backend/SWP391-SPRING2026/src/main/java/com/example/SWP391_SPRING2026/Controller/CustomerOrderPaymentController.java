package com.example.SWP391_SPRING2026.Controller;

import com.example.SWP391_SPRING2026.DTO.Response.PayRemainingResponseDTO;
import com.example.SWP391_SPRING2026.Entity.Order;
import com.example.SWP391_SPRING2026.Entity.Payment;
import com.example.SWP391_SPRING2026.Entity.UserPrincipal;
import com.example.SWP391_SPRING2026.Enum.*;
import com.example.SWP391_SPRING2026.Exception.BadRequestException;
import com.example.SWP391_SPRING2026.Repository.OrderRepository;
import com.example.SWP391_SPRING2026.Repository.PaymentRepository;
import com.example.SWP391_SPRING2026.Service.PreOrderService;
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
    private final PaymentRepository paymentRepository;
    private final VNPayService vnPayService;
    private final PreOrderService preOrderService;

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

        if (!preOrderService.isRemainingPaymentOpened(orderId)) {
            throw new BadRequestException("Remaining payment is not opened yet. Wait until stock arrives.");
        }

        if (order.getOrderStatus() == OrderStatus.SHIPPING
                || order.getOrderStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("Cannot pay remaining after shipping started");
        }

        Payment existingRemaining = paymentRepository
                .findTopByOrder_IdAndStageOrderByIdDesc(orderId, PaymentStage.REMAINING)
                .orElse(null);

        if (existingRemaining != null) {
            if (existingRemaining.getStatus() == PaymentStatus.SUCCESS) {
                throw new BadRequestException("Remaining already paid");
            }

            if (existingRemaining.getStatus() == PaymentStatus.PENDING
                    || existingRemaining.getStatus() == PaymentStatus.UNPAID) {
                existingRemaining.setStatus(PaymentStatus.CANCELLED);
            }
        }

        order.setRemainingPaymentMethod(PaymentMethod.VNPAY);

        Payment pay = new Payment();
        pay.setOrder(order);
        pay.setStage(PaymentStage.REMAINING);
        pay.setMethod(PaymentMethod.VNPAY);
        pay.setAmount(remaining);
        pay.setStatus(PaymentStatus.PENDING);
        pay.setCreatedAt(LocalDateTime.now());
        pay.setExpiresAt(LocalDateTime.now().plusMinutes(15));

        paymentRepository.save(pay);

        String ipAddress = request.getRemoteAddr();
        if (ipAddress == null || ipAddress.equals("0:0:0:0:0:0:0:1")) {
            ipAddress = "127.0.0.1";
        }

        String paymentUrl;
        try {
            paymentUrl = vnPayService.createVNPayUrl(
                    pay.getId().toString(),
                    remaining,
                    ipAddress
            );
        } catch (Exception e) {
            throw new RuntimeException("Cannot create VNPay URL");
        }

        return new PayRemainingResponseDTO(orderId, pay.getId(), remaining, paymentUrl);
    }
}