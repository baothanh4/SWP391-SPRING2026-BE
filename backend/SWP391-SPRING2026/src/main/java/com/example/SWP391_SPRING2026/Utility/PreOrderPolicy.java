package com.example.SWP391_SPRING2026.Utility;

import com.example.SWP391_SPRING2026.Enum.PreOrderPaymentOption;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PreOrderPolicy {
    private PreOrderPaymentOption option;
    private int minDepositPercent;

    public long calculateMinDeposit(long totalAmount) {
        return totalAmount * minDepositPercent / 100;
    }

    public static PreOrderPolicy noPreorder() {
        return new PreOrderPolicy(null, 0);
    }
}
