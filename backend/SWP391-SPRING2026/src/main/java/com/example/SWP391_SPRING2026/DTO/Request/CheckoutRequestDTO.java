package com.example.SWP391_SPRING2026.DTO.Request;

import com.example.SWP391_SPRING2026.Enum.OrderType;
import com.example.SWP391_SPRING2026.Enum.PaymentMethod;
import lombok.Data;

@Data
public class CheckoutRequestDTO {
    private Long addressId;
    private PaymentMethod paymentMethod;
}
