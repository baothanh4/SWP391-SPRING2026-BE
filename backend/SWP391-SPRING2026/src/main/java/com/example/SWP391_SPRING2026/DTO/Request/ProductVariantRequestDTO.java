package com.example.SWP391_SPRING2026.DTO.Request;

import com.example.SWP391_SPRING2026.Enum.SaleType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductVariantRequestDTO {
    private String sku;
    private BigDecimal price;
    private Integer stockQuantity;
    private SaleType saleType;
}

