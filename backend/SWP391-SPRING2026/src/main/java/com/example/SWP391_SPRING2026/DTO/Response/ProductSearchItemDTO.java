package com.example.SWP391_SPRING2026.DTO.Response;

import com.example.SWP391_SPRING2026.Enum.SaleType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Set;

@Data
public class ProductSearchItemDTO {
    private Long id;
    private String name;
    private String brandName;
    private String status;
    private String productImage;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private Set<SaleType> saleTypes;
    private Long totalStock;
    private Boolean hasStock;
}
