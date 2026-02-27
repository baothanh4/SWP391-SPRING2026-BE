package com.example.SWP391_SPRING2026.Utility;

import com.example.SWP391_SPRING2026.Enum.RefundPolicy;

public final class RefundCalculator {

    private RefundCalculator() {}

    // DEPOSIT_FORFEIT: refund = max(0, paid - deposit)
    // FULL_REFUND: refund = paid
    public static long calculate(long paidAmount, long depositAmount, RefundPolicy policy) {
        if (paidAmount < 0) paidAmount = 0;
        if (depositAmount < 0) depositAmount = 0;

        if (policy == RefundPolicy.FULL_REFUND) return paidAmount;

        long refund = paidAmount - depositAmount;
        return Math.max(refund, 0);
    }
}