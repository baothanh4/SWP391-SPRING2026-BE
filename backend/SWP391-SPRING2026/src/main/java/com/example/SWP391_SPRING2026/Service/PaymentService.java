package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Response.PaymentHistoryResponseDTO;
import com.example.SWP391_SPRING2026.Entity.Order;
import com.example.SWP391_SPRING2026.Entity.Payment;
import com.example.SWP391_SPRING2026.Enum.*;
import com.example.SWP391_SPRING2026.Repository.OrderRepository;
import com.example.SWP391_SPRING2026.Repository.PaymentRepository;
import com.example.SWP391_SPRING2026.Utility.VNPayUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final VNPayService vnPayService;
    private final PreOrderService preOrderService;

    private static final String SECRET_KEY =
            "4LTI2QLZGKBVC0HB79O3K437RSDFJDJJ";

    @Transactional
    public String handleVnpayReturn(HttpServletRequest request) {

        Map<String, String> params = VNPayUtils.getVNPayResponseParams(request);
        boolean valid = VNPayUtils.verifySignature(params, SECRET_KEY);

        if (!valid) {
            return "http://localhost:5173/payment-result?status=invalid";
        }

        String paymentIdStr = params.get("vnp_TxnRef");

        Payment payment = paymentRepository.findById(Long.parseLong(paymentIdStr))
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return "http://localhost:5173/payment-result?status=success";
        }

        Order order = payment.getOrder();

        if ("00".equals(params.get("vnp_ResponseCode"))) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionCode(params.get("vnp_TransactionNo"));
            payment.setPaidAt(LocalDateTime.now());

            if (order.getOrderType() == OrderType.PRE_ORDER) {
                if (payment.getStage() == PaymentStage.DEPOSIT
                        || payment.getStage() == PaymentStage.FULL) {
                    preOrderService.markInitialPaymentSuccess(order.getId());
                } else if (payment.getStage() == PaymentStage.REMAINING) {
                    order.setRemainingAmount(0L);
                    order.setRemainingPaymentMethod(null);
                    preOrderService.markRemainingPaid(order.getId());
                }
            }

        } else {
            payment.setStatus(PaymentStatus.FAILED);

            // fail initial payment => fail preorder order + release slot
            if (order.getOrderType() == OrderType.PRE_ORDER
                    && (payment.getStage() == PaymentStage.DEPOSIT || payment.getStage() == PaymentStage.FULL)
                    && !paymentRepository.existsByOrder_IdAndStatus(order.getId(), PaymentStatus.SUCCESS)) {

                order.setOrderStatus(OrderStatus.FAILED);

                if (order.getShipment() != null) {
                    order.getShipment().setStatus(ShipmentStatus.CANCELLED);
                }

                preOrderService.releaseReservations(order);
            }
        }

        paymentRepository.save(payment);

        return "http://localhost:5173/payment-result?status="
                + payment.getStatus().name().toLowerCase()
                + "?transactionCode="+payment.getTransactionCode()
                + "?amount="+payment.getAmount();
    }

    @Transactional
    public List<PaymentHistoryResponseDTO> getPaymentHistory(Long userId){
        List<Payment> payments = paymentRepository.findByUserId(userId);

        return payments.stream()
                .map(p -> new PaymentHistoryResponseDTO(
                        p.getId(),
                        p.getOrder().getOrderCode(),
                        p.getStage(),
                        p.getMethod(),
                        p.getStatus(),
                        p.getAmount(),
                        p.getTransactionCode(),
                        p.getCreatedAt(),
                        p.getPaidAt()
                )).toList();
    }
}