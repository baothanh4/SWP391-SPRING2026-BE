package com.example.SWP391_SPRING2026.Enum;

public enum PaymentStage {
    FULL,       // IN_STOCK hoặc PRE_ORDER trả 100%
    DEPOSIT,    // PRE_ORDER trả cọc
    REMAINING   // PRE_ORDER trả nốt
}