package com.example.SWP391_SPRING2026.Enum;

public enum PreOrderStatus {
    RESERVED,                   // vừa checkout, đang giữ slot tạm
    AWAITING_STOCK,             // cọc/đã thanh toán xong, chờ hàng về
    AWAITING_REMAINING_PAYMENT, // hàng đã về, chờ trả nốt
    READY_FOR_PROCESSING,       // đã trả đủ, sẵn sàng xử lý
    READY_TO_SHIP,              // ops đã verify xong, sẵn sàng đẩy GHN
    CANCELLED,
    FULFILLED
}