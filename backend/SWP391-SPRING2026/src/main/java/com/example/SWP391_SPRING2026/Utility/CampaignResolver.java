package com.example.SWP391_SPRING2026.Utility;

import com.example.SWP391_SPRING2026.Entity.PreOrderCampaign;
import com.example.SWP391_SPRING2026.Entity.ProductVariant;

import java.time.LocalDate;

public class CampaignResolver {
    public static PreOrderCampaign getActiveCampaign(ProductVariant variant) {
        LocalDate today = LocalDate.now();

        return variant.getCampaignVariants().stream()
                .map(link -> link.getCampaign())
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .filter(c -> !today.isBefore(c.getStartDate()) && !today.isAfter(c.getEndDate()))
                .findFirst()
                .orElse(null);
    }

    public static PreOrderCampaign getLatestCampaign(ProductVariant variant) {
        return variant.getCampaignVariants().stream()
                .map(link -> link.getCampaign())
                .max((a, b) -> a.getEndDate().compareTo(b.getEndDate()))
                .orElse(null);
    }
}
