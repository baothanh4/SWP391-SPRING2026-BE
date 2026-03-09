package com.example.SWP391_SPRING2026.DTO.Response;

import com.example.SWP391_SPRING2026.Enum.OrderStatus;
import com.example.SWP391_SPRING2026.Enum.PreOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@Data
@AllArgsConstructor
public class RemainingPaymentStatusResponseDTO {
    private Long orderId;
    private OrderStatus orderStatus;
    private Long remainingAmount;
    private boolean opened;
    private Set<PreOrderStatus> preorderStatuses;
}