package com.example.SWP391_SPRING2026.DTO.Request;

import com.example.SWP391_SPRING2026.Enum.PaymentMethod;
import lombok.Data;

@Data
public class CheckoutRequestDTO {
    private Long addressId;

    private PaymentMethod paymentMethod;

    // option chọn số tiền cọc (>= 30%, có thể = 100%)
    private Long depositAmount;

    // pre-order trả nốt: COD hoặc VNPAY
    private PaymentMethod remainingPaymentMethod;
}