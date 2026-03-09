package com.example.SWP391_SPRING2026.DTO.Response;

import com.example.SWP391_SPRING2026.Enum.SaleType;
import com.example.SWP391_SPRING2026.Enum.VariantAvailabilityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponseDTO {
    private Long id;
    private String sku;
    private BigDecimal price;
    private Integer stockQuantity;
    private SaleType saleType;

    private Boolean allowPreorder;
    private Integer preorderLimit;
    private Integer currentPreorders;
    private LocalDate preorderFulfillmentDate;
    private VariantAvailabilityStatus availabilityStatus;

    private Long product_id;
}