package com.example.SWP391_SPRING2026.Utility;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class DepositPolicy {
    private DepositPolicy() {}

    public static final BigDecimal MIN_DEPOSIT_RATE = new BigDecimal("0.30");

    /**
     * Min deposit = ceil(total * 30%)
     * CEILING để tránh case total lẻ -> (total * 30 / 100) bị làm tròn xuống < 30%
     */
    public static long minDeposit(long totalAmount) {
        if (totalAmount <= 0) return 0L;

        return BigDecimal.valueOf(totalAmount)
                .multiply(MIN_DEPOSIT_RATE)
                .setScale(0, RoundingMode.CEILING)
                .longValueExact();
    }

    public static void validate(long totalAmount, long depositAmount) {
        if (depositAmount < 0) {
            throw new IllegalArgumentException("Deposit cannot be negative");
        }
        if (depositAmount > totalAmount) {
            throw new IllegalArgumentException("Deposit cannot be greater than total");
        }
        long min = minDeposit(totalAmount);
        if (depositAmount < min) {
            throw new IllegalArgumentException("Deposit must be at least 30% of total");
        }
    }
}