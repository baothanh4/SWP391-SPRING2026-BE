package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Request.CreateGhnOrderRequest;
import com.example.SWP391_SPRING2026.Entity.Address;
import com.example.SWP391_SPRING2026.Entity.Order;
import com.example.SWP391_SPRING2026.Enum.PaymentMethod;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GhnService {

    private final RestTemplate restTemplate;

    private final String token = "ede20835-0d23-11f1-a3d6-dac90fb956b5";
    private final String shopId = "199365";
    private final String baseUrl = "https://dev-online-gateway.ghn.vn/shiip/public-api";

    // ================= HEADERS =================
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", token);
        headers.set("ShopId", shopId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    // ================= MASTER DATA =================

    public String getDistricts(int provinceId) {
        String url = baseUrl + "/v2/master-data/district?province_id=" + provinceId;

        HttpEntity<?> entity = new HttpEntity<>(createHeaders());

        ResponseEntity<String> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        return response.getBody();
    }


    public String getWards(int districtId) {

        String url = baseUrl + "/v2/master-data/ward";

        String body = """
            {
              "district_id": %d
            }
            """.formatted(districtId);

        HttpEntity<String> entity =
                new HttpEntity<>(body, createHeaders());

        ResponseEntity<String> response =
                restTemplate.postForEntity(url, entity, String.class);

        return response.getBody();
    }

    // ================= CREATE ORDER =================

    public String createOrder(Order order) {

        CreateGhnOrderRequest request = buildRequest(order);

        String url = baseUrl + "/v2/shipping-order/create";

        HttpEntity<CreateGhnOrderRequest> entity =
                new HttpEntity<>(request, createHeaders());

        ResponseEntity<String> response =
                restTemplate.postForEntity(url, entity, String.class);

        return extractOrderCode(response.getBody());
    }

    private CreateGhnOrderRequest buildRequest(Order order) {

        CreateGhnOrderRequest request = new CreateGhnOrderRequest();

        // ===== PAYMENT =====
        Long cod = (order.getShipment() != null) ? order.getShipment().getCodAmount() : null;

        if (cod != null && cod > 0) {
            request.setPayment_type_id(2);
            request.setCod_amount(cod.intValue());
        } else {
            request.setPayment_type_id(1);
            request.setCod_amount(0);
        }

        // ===== SHOP INFO (HARDCODE DEMO) =====
        request.setFrom_name("SWP391 Shop");
        request.setFrom_phone("0901234567");
        request.setFrom_address("123 Nguyen Trai");
        request.setFrom_district_id(1454); // Quận 5
        request.setFrom_ward_code("21211"); // Phường 7 (ví dụ)

        // ===== CUSTOMER =====
        Address address = order.getAddress();

        request.setTo_name(address.getReceiverName());
        request.setTo_phone(normalizePhone(address.getPhone()));
        request.setTo_address(address.getAddressLine());

        // ⚠️ PHẢI LƯU district_id + ward_code TRONG DB
        request.setTo_district_id(address.getDistrictId());
        request.setTo_ward_code(address.getWardCode());

        // ===== PACKAGE =====
        request.setWeight(1000);
        request.setLength(20);
        request.setWidth(20);
        request.setHeight(10);
        request.setService_type_id(2);

        // ===== ITEMS =====
        List<CreateGhnOrderRequest.Item> items =
                order.getOrderItems().stream().map(orderItem -> {

                    CreateGhnOrderRequest.Item item =
                            new CreateGhnOrderRequest.Item();

                    item.setName(orderItem.getProductVariant().getProduct().getName());
                    item.setQuantity(orderItem.getQuantity());
                    item.setPrice(orderItem.getPrice().intValue());

                    item.setLength(20);
                    item.setWidth(20);
                    item.setHeight(5);
                    item.setWeight(500);

                    return item;

                }).toList();

        request.setItems(items);

        return request;
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;

        phone = phone.trim().replaceAll("\\s+", "");

        if (phone.startsWith("+84")) {
            phone = "0" + phone.substring(3);
        }

        return phone;
    }

    private String extractOrderCode(String responseBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseBody);

            return root.path("data")
                    .path("order_code")
                    .asText();

        } catch (Exception e) {
            throw new RuntimeException("Cannot parse GHN response", e);
        }
    }
}
