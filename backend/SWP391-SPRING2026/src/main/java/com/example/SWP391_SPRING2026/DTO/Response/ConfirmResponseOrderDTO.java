package com.example.SWP391_SPRING2026.DTO.Response;

import com.example.SWP391_SPRING2026.Enum.ApprovalStatus;
import com.example.SWP391_SPRING2026.Enum.OrderStatus;
import com.example.SWP391_SPRING2026.Enum.ShipmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConfirmResponseOrderDTO {
    private String orderCode;
    private String ghnOrderCode;
    private ShipmentStatus shipmentStatus;
    private OrderStatus orderStatus;

    private ApprovalStatus approvalStatus;
    private LocalDateTime supportApprovedAt;
    private LocalDateTime operationConfirmedAt;
}