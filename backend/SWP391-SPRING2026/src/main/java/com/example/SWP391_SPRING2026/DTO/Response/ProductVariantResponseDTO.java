package com.example.SWP391_SPRING2026.DTO.Response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductVariantResponseDTO {
    private Long id;
    private String sku;
    private BigDecimal price;
    private Integer stockQuantity;
}
