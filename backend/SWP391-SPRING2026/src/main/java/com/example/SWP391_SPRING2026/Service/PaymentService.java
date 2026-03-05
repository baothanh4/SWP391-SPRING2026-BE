package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Response.PaymentHistoryResponseDTO;
import com.example.SWP391_SPRING2026.Entity.Order;
import com.example.SWP391_SPRING2026.Entity.Payment;
import com.example.SWP391_SPRING2026.Enum.PaymentMethod;
import com.example.SWP391_SPRING2026.Enum.PaymentStage;
import com.example.SWP391_SPRING2026.Enum.PaymentStatus;
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

    private static final String SECRET_KEY =
            "4LTI2QLZGKBVC0HB79O3K437RSDFJDJJ";

    // =====================================================
    // 1️⃣ CREATE NEW VNPAY PAYMENT (1–N SAFE)
    // =====================================================
    @Transactional
    public String createVNPayPayment(Long orderId,
                                     HttpServletRequest request) throws Exception {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // ❗ Kiểm tra đã có SUCCESS payment chưa
        boolean alreadyPaid = paymentRepository
                .existsByOrder_IdAndStatus(orderId, PaymentStatus.SUCCESS);

        if (alreadyPaid) {
            throw new RuntimeException("Order already paid");
        }

        // 🔥 Tạo payment mới (KHÔNG overwrite cái cũ)
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setStage(PaymentStage.FULL); // nếu PRE_ORDER thì xử lý riêng
        payment.setMethod(PaymentMethod.VNPAY);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        // Lấy IP chuẩn
        String ipAddress = request.getRemoteAddr();
        if (ipAddress == null || ipAddress.equals("0:0:0:0:0:0:0:1")) {
            ipAddress = "127.0.0.1";
        }

        // 🔥 Quan trọng: truyền paymentId làm TxnRef
        return vnPayService.createVNPayUrl(
                payment.getId().toString(),
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

        Payment payment = paymentRepository.findById(Long.parseLong(paymentIdStr))
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // Nếu đã SUCCESS rồi thì không update lại
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return "http://localhost:5173/payment-result?status=success";
        }

        if ("00".equals(params.get("vnp_ResponseCode"))) {

            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setTransactionCode(params.get("vnp_TransactionNo"));
            payment.setPaidAt(LocalDateTime.now());

        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }

        paymentRepository.save(payment);

        return "http://localhost:5173/payment-result?status="
                + payment.getStatus().name().toLowerCase();
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