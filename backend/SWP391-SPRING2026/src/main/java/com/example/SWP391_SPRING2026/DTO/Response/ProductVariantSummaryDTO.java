package com.example.SWP391_SPRING2026.DTO.Response;

import com.example.SWP391_SPRING2026.Enum.SaleType;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantSummaryDTO {
    private Long variantId;
    private BigDecimal price;
    private Integer stockQuantity;
    private SaleType saleType;
    private Set<Long> imageIds;
}
