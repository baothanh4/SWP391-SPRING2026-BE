package com.example.SWP391_SPRING2026.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PayRemainingResponseDTO {
    private Long orderId;
    private Long paymentId;
    private Long amount;
    private String paymentUrl;
}