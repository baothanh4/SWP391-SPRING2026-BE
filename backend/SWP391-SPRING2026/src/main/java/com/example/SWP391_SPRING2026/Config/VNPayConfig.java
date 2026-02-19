package com.example.SWP391_SPRING2026.Config;

public class VNPayConfig {
    public static final String VNP_TMN_CODE = "0LV63K8R";
    public static final String VNP_HASH_SECRET = "4LTI2QLZGKBVC0HB79O3K437RSDFJDJJ";

    public static final String VNP_PAY_URL =
            "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    public static final String VNP_RETURN_URL =
            "http://localhost:8081/api/payment/vnpay-return";

    public static final String VNP_IPN_URL =
            "http://localhost:8081/api/payment/vnpay-ipn";
}
