package com.example.SWP391_SPRING2026.DTO.Request;

import com.example.SWP391_SPRING2026.Enum.PaymentMethod;
import lombok.Data;

@Data
public class CheckoutRequestDTO {
    private Long addressId;

    private PaymentMethod paymentMethod;

    // pre-order: có thể gửi số tiền cọc thực tế hoặc phần trăm (1..100), BE sẽ map theo rule campaign
    private Long depositAmount;

    // pre-order trả nốt: COD hoặc VNPAY
    private PaymentMethod remainingPaymentMethod;
}