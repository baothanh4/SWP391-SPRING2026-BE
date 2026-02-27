package com.example.SWP391_SPRING2026.DTO.Request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ComboItemRequestDTO {
    @NotNull(message = "VariantId is required")
    private Long variantId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be >=1")
    private Integer quantity;
}
