package com.example.SWP391_SPRING2026.Enum;

public enum OrderStatus {
    PENDING,
    PAID,
    SHIPPING,
    COMPLETED,
    CANCELLED,
    PENDING_PAYMENT,
    WAITING_CONFIRM,
    CONFIRMED,
    SUPPORT_CONFIRMED,
    OPERATION_CONFIRMED,
    FAILED;

    public boolean canCancelByCustomer() {
        return this == WAITING_CONFIRM;
    }

    public boolean canCancelByStaff() {
        return this != COMPLETED && this != CANCELLED;
    }

    public boolean canBeConfirmedBySupport() {
        return this == WAITING_CONFIRM;
    }

    public boolean canShip() {
        return this == SUPPORT_CONFIRMED;
    }
}
