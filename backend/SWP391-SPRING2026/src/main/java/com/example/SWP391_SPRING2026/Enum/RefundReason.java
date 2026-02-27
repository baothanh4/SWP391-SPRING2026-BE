package com.example.SWP391_SPRING2026.Enum;

public enum RefundReason {
    CUSTOMER_CANCEL,      // khách tự hủy
    SHOP_CANNOT_SUPPLY,   // shop không cung cấp được
    LATE_DELIVERY,        // giao trễ cam kết (khung, làm sau)
    SYSTEM_ERROR          // lỗi hệ thống
}
