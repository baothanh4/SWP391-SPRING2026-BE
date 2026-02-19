package com.example.SWP391_SPRING2026.Utility;

import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class VNPayUtils {

    public static boolean verifySignature(
            Map<String, String> fields,
            String secretKey) {

        SortedMap<String, String> sorted = new TreeMap<>();

        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (entry.getKey().startsWith("vnp_")) {
                sorted.put(entry.getKey(), entry.getValue());
            }
        }

        String receivedHash = sorted.remove("vnp_SecureHash");
        sorted.remove("vnp_SecureHashType");

        StringBuilder hashData = new StringBuilder();

        for (Map.Entry<String, String> entry : sorted.entrySet()) {

            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                hashData.append(entry.getKey())
                        .append("=")
                        .append(URLEncoder.encode(
                                entry.getValue(),
                                StandardCharsets.UTF_8))
                        .append("&");
            }
        }


        if (hashData.length() > 0) {
            hashData.deleteCharAt(hashData.length() - 1);
        }

        String rawData = hashData.toString();

        String calculatedHash =
                HmacUtil.hmacSHA512(secretKey, rawData);

        // 🔥 IN LOG DEBUG
        System.out.println("========== VNPAY DEBUG ==========");
        System.out.println("RAW DATA:");
        System.out.println(rawData);
        System.out.println("CALCULATED HASH:");
        System.out.println(calculatedHash);
        System.out.println("RECEIVED HASH:");
        System.out.println(receivedHash);
        System.out.println("==================================");

        return calculatedHash.equalsIgnoreCase(receivedHash);
    }



    public static Map<String, String> getVNPayResponseParams(
            HttpServletRequest request) {

        Map<String, String> fields = new HashMap<>();

        Enumeration<String> paramNames = request.getParameterNames();

        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String paramValue = request.getParameter(paramName);

            if (paramValue != null && !paramValue.isEmpty()) {
                fields.put(paramName, paramValue);
            }
        }

        return fields;
    }



}
