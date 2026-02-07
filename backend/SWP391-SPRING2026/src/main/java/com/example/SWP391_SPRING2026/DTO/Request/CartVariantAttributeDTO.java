package com.example.SWP391_SPRING2026.DTO.Request;

import lombok.Data;

import java.util.List;

@Data
public class CartVariantAttributeDTO {
    private String attributeName;
    private String attributeValue;
    private List<String> images;
}
