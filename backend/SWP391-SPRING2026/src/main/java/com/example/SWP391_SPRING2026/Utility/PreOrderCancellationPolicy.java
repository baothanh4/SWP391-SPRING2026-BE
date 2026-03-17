package com.example.SWP391_SPRING2026.Utility;

import java.time.LocalDate;

public final class PreOrderCancellationPolicy {
    private PreOrderCancellationPolicy() {}

    public static boolean isFullRefundEligible(LocalDate today, LocalDate preorderEndDate) {
        LocalDate deadline = preorderEndDate.minusDays(PreOrderRule.FULL_REFUND_DEADLINE_DAYS_BEFORE_END);
        return !today.isAfter(deadline);
    }
}