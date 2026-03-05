package com.example.SWP391_SPRING2026.DTO.Response;

import com.example.SWP391_SPRING2026.Enum.ReturnReason;
import com.example.SWP391_SPRING2026.Enum.ReturnRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ReturnRequestResponseDTO {
    private Long id;
    private Long orderId;
    private Long orderItemId;
    private Integer requestedQuantity;
    private Integer acceptedQuantity; // null nếu chưa received
    private ReturnReason reason;
    private ReturnRequestStatus status;
    private String note;
    private String evidenceUrls;
    private LocalDateTime createdAt;
}