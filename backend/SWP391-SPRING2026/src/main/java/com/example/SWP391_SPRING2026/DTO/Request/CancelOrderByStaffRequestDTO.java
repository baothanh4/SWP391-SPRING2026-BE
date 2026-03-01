package com.example.SWP391_SPRING2026.DTO.Request;

import com.example.SWP391_SPRING2026.Enum.RefundReason;
import lombok.Data;

@Data
public class CancelOrderByStaffRequestDTO {
    private RefundReason reason; // SHOP_CANNOT_SUPPLY | SYSTEM_ERROR | LATE_DELIVERY
    private String note;
}
