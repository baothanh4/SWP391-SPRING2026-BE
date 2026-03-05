package com.example.SWP391_SPRING2026.DTO.Request;

import com.example.SWP391_SPRING2026.Enum.ReturnReason;
import lombok.Data;

@Data
public class SubmitReturnRequestDTO {
    private Long orderItemId;
    private Integer quantity;
    private ReturnReason reason;
    private String note;
    private String evidenceUrls;
}