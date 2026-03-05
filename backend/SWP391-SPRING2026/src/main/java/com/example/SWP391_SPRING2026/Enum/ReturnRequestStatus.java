package com.example.SWP391_SPRING2026.Enum;

public enum ReturnRequestStatus {
    SUBMITTED,        // customer tạo
    WAITING_RETURN,   // support duyệt xong, đợi gửi hàng
    RECEIVED,         // operation nhận hàng
    REFUND_REQUESTED, // đã tạo RefundRequest
    REJECTED,         // support từ chối
    CANCELLED         // customer hủy trước khi duyệt
}
