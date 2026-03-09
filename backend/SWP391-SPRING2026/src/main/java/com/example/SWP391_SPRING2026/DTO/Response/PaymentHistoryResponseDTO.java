package com.example.SWP391_SPRING2026.DTO.Response;

import com.example.SWP391_SPRING2026.Enum.PaymentMethod;
import com.example.SWP391_SPRING2026.Enum.PaymentStage;
import com.example.SWP391_SPRING2026.Enum.PaymentStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentHistoryResponseDTO {
    private Long paymentId;
    private String orderCode;

    private PaymentStage stage;
    private PaymentMethod method;
    private PaymentStatus status;

    private Long amount;
    private String transactionCode;

    private LocalDateTime createAt;
    private LocalDateTime paidAt;
}
