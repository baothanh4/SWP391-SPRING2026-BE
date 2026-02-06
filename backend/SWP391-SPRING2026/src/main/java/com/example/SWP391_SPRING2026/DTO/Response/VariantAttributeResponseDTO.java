package com.example.SWP391_SPRING2026.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VariantAttributeResponseDTO {
    private Long id;
    private String attributeName;
    private String attributeValue;
    private List<String> images;
}
