package com.example.SWP391_SPRING2026.Utility;

import com.example.SWP391_SPRING2026.Entity.ProductVariant;
import com.example.SWP391_SPRING2026.Enum.SaleType;
import com.example.SWP391_SPRING2026.Enum.VariantAvailabilityStatus;

public final class VariantAvailabilityResolver {

    private VariantAvailabilityResolver() {
    }

    public static VariantAvailabilityStatus resolve(ProductVariant variant) {
        SaleType saleType = variant.getSaleType();
        int stock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();

        if (saleType == SaleType.PRE_ORDER) {
            boolean allow = Boolean.TRUE.equals(variant.getAllowPreorder());
            int limit = variant.getPreorderLimit() == null ? 0 : variant.getPreorderLimit();
            int current = variant.getCurrentPreorders() == null ? 0 : variant.getCurrentPreorders();

            if (!allow) return VariantAvailabilityStatus.OUT_OF_STOCK;
            if (limit <= 0) return VariantAvailabilityStatus.OUT_OF_STOCK;
            if (current >= limit) return VariantAvailabilityStatus.OUT_OF_STOCK;
            if (variant.getPreorderFulfillmentDate() == null) return VariantAvailabilityStatus.OUT_OF_STOCK;

            return VariantAvailabilityStatus.PRE_ORDER;
        }

        if (stock > 0) return VariantAvailabilityStatus.IN_STOCK;
        return VariantAvailabilityStatus.OUT_OF_STOCK;
    }
}