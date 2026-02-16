package com.example.SWP391_SPRING2026.DTO.Request;

import lombok.Data;

import java.util.List;

@Data
public class CreateGhnOrderRequest {
    private String payment_type_id; // 1: người gửi trả, 2: người nhận trả
    private String note;
    private String required_note;

    private String from_name;
    private String from_phone;
    private String from_address;
    private String from_ward_name;
    private String from_district_name;
    private String from_province_name;

    private String to_name;
    private String to_phone;
    private String to_address;
    private String to_ward_name;
    private String to_district_name;
    private String to_province_name;

    private int weight;
    private int length;
    private int width;
    private int height;

    private int service_type_id;

    private int cod_amount;

    private List<Item> items;

    @Data
    public static class Item {
        private String name;
        private int quantity;
        private int price;
        private int length;
        private int width;
        private int height;
        private int weight;
    }
}
