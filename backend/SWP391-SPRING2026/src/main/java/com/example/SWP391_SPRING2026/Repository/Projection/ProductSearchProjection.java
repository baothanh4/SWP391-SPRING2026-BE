package com.example.SWP391_SPRING2026.Repository.Projection;

import com.example.SWP391_SPRING2026.Enum.ProductStatus;

import java.math.BigDecimal;
import java.util.Set;

public interface ProductSearchProjection {
    Long getId();
    String getName();
    String getBrandName();
    ProductStatus getStatus();
    String getProductImage();

    BigDecimal getMinPrice();
    BigDecimal getMaxPrice();
    Set<String> getSaleTypes();
    Long getTotalStock();
}
