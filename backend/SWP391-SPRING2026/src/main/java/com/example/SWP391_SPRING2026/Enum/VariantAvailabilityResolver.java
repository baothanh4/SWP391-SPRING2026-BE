package com.example.SWP391_SPRING2026.Utility;

import com.example.SWP391_SPRING2026.Entity.ProductVariant;
import com.example.SWP391_SPRING2026.Enum.VariantAvailabilityStatus;

public final class VariantAvailabilityResolver {

    private VariantAvailabilityResolver() {
    }

    public static VariantAvailabilityStatus resolve(ProductVariant variant) {
        int stock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
        if (stock > 0) {
            return VariantAvailabilityStatus.IN_STOCK;
        }

        boolean allow = Boolean.TRUE.equals(variant.getAllowPreorder());
        int limit = variant.getPreorderLimit() == null ? 0 : variant.getPreorderLimit();
        int current = variant.getCurrentPreorders() == null ? 0 : variant.getCurrentPreorders();

        if (!allow) {
            return VariantAvailabilityStatus.OUT_OF_STOCK;
        }

        if (limit <= 0) {
            return VariantAvailabilityStatus.OUT_OF_STOCK;
        }

        if (current >= limit) {
            return VariantAvailabilityStatus.OUT_OF_STOCK;
        }

        if (variant.getPreorderFulfillmentDate() == null) {
            return VariantAvailabilityStatus.OUT_OF_STOCK;
        }

        return VariantAvailabilityStatus.PRE_ORDER;
    }
}