package com.example.SWP391_SPRING2026.DTO.Response;

import com.example.SWP391_SPRING2026.Enum.RefundPolicy;
import com.example.SWP391_SPRING2026.Enum.RefundReason;
import com.example.SWP391_SPRING2026.Enum.RefundRequestStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RefundRequestResponseDTO {
    private Long id;
    private Long orderId;
    private RefundReason reason;
    private RefundPolicy policy;
    private RefundRequestStatus status;
    private Long refundAmount;
    private String note;
    private Long createdByUserId;
    private String createdByRole;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}