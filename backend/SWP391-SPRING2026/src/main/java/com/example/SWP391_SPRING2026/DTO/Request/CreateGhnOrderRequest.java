package com.example.SWP391_SPRING2026.DTO.Request;

import lombok.Data;
import java.util.List;

@Data
public class CreateGhnOrderRequest {

    private Integer payment_type_id;
    private Integer cod_amount;

    private String note;
    private String required_note;

    // FROM
    private String from_name;
    private String from_phone;
    private String from_address;
    private String from_ward_code;
    private Integer from_district_id;

    // TO
    private String to_name;
    private String to_phone;
    private String to_address;
    private String to_ward_code;
    private Integer to_district_id;

    // PACKAGE
    private Integer weight;
    private Integer length;
    private Integer width;
    private Integer height;
    private Integer service_type_id;

    private List<Item> items;

    @Data
    public static class Item {
        private String name;
        private Integer quantity;
        private Integer price;
        private Integer length;
        private Integer width;
        private Integer height;
        private Integer weight;
    }
}
