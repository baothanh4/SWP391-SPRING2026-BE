package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.Entity.Order;
import com.example.SWP391_SPRING2026.Entity.Payment;
import com.example.SWP391_SPRING2026.Enum.PaymentMethod;
import com.example.SWP391_SPRING2026.Enum.PaymentStatus;
import com.example.SWP391_SPRING2026.Repository.OrderRepository;
import com.example.SWP391_SPRING2026.Repository.PaymentRepository;
import com.example.SWP391_SPRING2026.Utility.VNPayUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.SWP391_SPRING2026.Entity.OrderPayment;
import com.example.SWP391_SPRING2026.Repository.OrderPaymentRepository;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final VNPayService vnPayService;
    private final OrderPaymentRepository orderPaymentRepository;

    private static final String SECRET_KEY =
            "4LTI2QLZGKBVC0HB79O3K437RSDFJDJJ";

    // =====================================================
    // 1️⃣ CREATE VNPAY PAYMENT URL
    // =====================================================
    @Transactional
    public String createVNPayPayment(Long orderId,
                                     HttpServletRequest request) throws Exception {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Order not found"));

        Payment payment = order.getPayment();

        if (payment == null) {
            throw new RuntimeException("Payment not found");
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new RuntimeException("Order already paid");
        }

        // Update trạng thái trước khi redirect
        payment.setMethod(PaymentMethod.VNPAY);
        payment.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(payment);

        // 🔥 LẤY IP CHUẨN (KHÔNG DÙNG LOCALHOST)
        String ipAddress = request.getRemoteAddr();
        if (ipAddress == null || ipAddress.equals("0:0:0:0:0:0:0:1")) {
            ipAddress = "127.0.0.1";
        }

        return vnPayService.createVNPayUrl(
                order.getId().toString(),
                payment.getAmount(),
                ipAddress
        );
    }

    // =====================================================
    // 2️⃣ HANDLE VNPAY RETURN
    // =====================================================
    @Transactional
    public String handleVnpayReturn(HttpServletRequest request) {

        Map<String, String> params =
                VNPayUtils.getVNPayResponseParams(request);

        boolean valid =
                VNPayUtils.verifySignature(params, SECRET_KEY);

        if (!valid) {
            return "http://localhost:5173/payment-result?status=invalid";
        }

        String paymentIdStr = params.get("vnp_TxnRef");

        OrderPayment pay = orderPaymentRepository.findById(Long.parseLong(paymentIdStr))
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if ("00".equals(params.get("vnp_ResponseCode"))) {

            pay.setStatus(PaymentStatus.SUCCESS);
            pay.setTransactionCode(params.get("vnp_TransactionNo"));
            pay.setPaidAt(LocalDateTime.now());

        } else {
            pay.setStatus(PaymentStatus.FAILED);
        }

        orderPaymentRepository.save(pay);

        return "http://localhost:5173/payment-result?status="
                + pay.getStatus().name().toLowerCase();

    }
}
