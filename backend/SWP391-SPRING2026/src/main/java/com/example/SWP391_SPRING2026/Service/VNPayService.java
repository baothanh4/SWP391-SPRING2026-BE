package com.example.SWP391_SPRING2026.Service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

import static com.example.SWP391_SPRING2026.Utility.HmacUtil.hmacSHA512;

@Service
public class VNPayService {

    private static final String TMN_CODE = "0LV63K8R";
    private static final String SECRET_KEY = "4LTI2QLZGKBVC0HB79O3K437RSDFJDJJ";
    private static final String VNP_URL =
            "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    public String createVNPayUrl(String orderId,
                                 long amount,
                                 String ipAddress) throws Exception {

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        String createDate =
                LocalDateTime.now().format(formatter);

        Map<String, String> params = new TreeMap<>();

        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", TMN_CODE);
        params.put("vnp_Amount", String.valueOf(amount * 100));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", orderId);
        params.put("vnp_OrderInfo",
                "Thanh toan don hang: " + orderId);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl",
                "http://localhost:8081/api/payment/vnpay-return");
        params.put("vnp_IpAddr", ipAddress);
        params.put("vnp_CreateDate", createDate);

        // ===== BUILD HASH DATA =====
        StringBuilder hashData = new StringBuilder();

        for (Map.Entry<String, String> entry : params.entrySet()) {

            if (entry.getValue() != null && !entry.getValue().isEmpty()) {

                hashData.append(entry.getKey())
                        .append("=")
                        .append(URLEncoder.encode(
                                entry.getValue(),
                                StandardCharsets.UTF_8))
                        .append("&");
            }
        }

        hashData.deleteCharAt(hashData.length() - 1);

        String secureHash =
                hmacSHA512(SECRET_KEY, hashData.toString());


        // ===== BUILD URL =====
        StringBuilder paymentUrl =
                new StringBuilder(VNP_URL).append("?");

        for (Map.Entry<String, String> entry : params.entrySet()) {

            paymentUrl.append(entry.getKey())
                    .append("=")
                    .append(URLEncoder.encode(
                            entry.getValue(),
                            StandardCharsets.UTF_8))
                    .append("&");
        }

        paymentUrl.append("vnp_SecureHash=")
                .append(secureHash);

        return paymentUrl.toString();
    }


}
