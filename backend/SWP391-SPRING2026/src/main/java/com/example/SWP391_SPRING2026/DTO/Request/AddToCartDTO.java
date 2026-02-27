package com.example.SWP391_SPRING2026.DTO.Request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddToCartDTO {
    private Long productVariantId;
    private Long productId;
    private Long productComboId;
    @NotNull
    @Min(1)
    private Integer quantity;


}
