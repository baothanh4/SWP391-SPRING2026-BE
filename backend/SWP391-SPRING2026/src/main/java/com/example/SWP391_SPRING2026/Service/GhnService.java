package com.example.SWP391_SPRING2026.Service;


import com.example.SWP391_SPRING2026.DTO.Request.CreateGhnOrderRequest;
import com.example.SWP391_SPRING2026.Entity.Address;
import com.example.SWP391_SPRING2026.Entity.Order;
import com.example.SWP391_SPRING2026.Enum.PaymentMethod;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpHeaders;
import java.util.List;


@Service
@RequiredArgsConstructor
public class GhnService {
    private final RestTemplate restTemplate;

    @Value("${ghn.token}")
    private String token;

    @Value("${ghn.shop-id}")
    private String shopId;

    @Value("${ghn.baseUrl}")
    private String baseUrl;

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", token);
        headers.set("ShopId", shopId);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public String createGhnOrder(CreateGhnOrderRequest request) {

        String url = baseUrl + "/v2/shipping-order/create";

        HttpEntity<CreateGhnOrderRequest> entity =
                new HttpEntity<>(request, createHeaders());

        ResponseEntity<JsonNode> response =
                restTemplate.postForEntity(url, entity, JsonNode.class);

        return response.getBody()
                .path("data")
                .path("order_code")
                .asText();
    }


    public CreateGhnOrderRequest buildRequest(Order order) {

        CreateGhnOrderRequest request = new CreateGhnOrderRequest();

        // ===== PAYMENT =====
        if (order.getPayment().getMethod() == PaymentMethod.COD) {
            request.setPayment_type_id("2"); // người nhận trả
            request.setCod_amount(
                    order.getRemainingAmount() != null
                            ? order.getRemainingAmount().intValue()
                            : order.getTotalAmount().intValue()
            );
        } else {
            request.setPayment_type_id("1"); // người gửi trả
            request.setCod_amount(0);
        }

        request.setNote("Giao giờ hành chính");
        request.setRequired_note("KHONGCHOXEMHANG");

        // ===== SHOP INFO =====
        request.setFrom_name("SWP391 Shop");
        request.setFrom_phone("0900000000");
        request.setFrom_address("123 Nguyen Trai");
        request.setFrom_ward_name("Phường 7");
        request.setFrom_district_name("Quận 5");
        request.setFrom_province_name("Hồ Chí Minh");

        // ===== CUSTOMER =====
        Address address = order.getAddress();

        request.setTo_name(address.getReceiverName());
        request.setTo_phone(address.getPhone());
        request.setTo_address(address.getAddressLine());
        request.setTo_ward_name(address.getWard());
        request.setTo_district_name(address.getDistrict());
        request.setTo_province_name(address.getProvince());

        // ===== PACKAGE SIZE =====
        request.setWeight(1000); // 1kg
        request.setLength(20);
        request.setWidth(20);
        request.setHeight(10);

        request.setService_type_id(2); // hàng thường

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

    public String createOrder(Order order) {

        CreateGhnOrderRequest request = buildRequest(order);

        String url = baseUrl + "/v2/shipping-order/create";

        HttpEntity<CreateGhnOrderRequest> entity =
                new HttpEntity<>(request, createHeaders());

        ResponseEntity<JsonNode> response =
                restTemplate.postForEntity(url, entity, JsonNode.class);

        return response.getBody()
                .path("data")
                .path("order_code")
                .asText();
    }




}
