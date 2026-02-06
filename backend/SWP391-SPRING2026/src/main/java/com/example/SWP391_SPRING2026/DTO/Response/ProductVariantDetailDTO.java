package com.example.SWP391_SPRING2026.DTO.Response;

import com.example.SWP391_SPRING2026.DTO.Request.VariantAttributeRequestDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductVariantDetailDTO {
    private Long id;
    private String sku;
    private BigDecimal price;
    private Integer stockQuantity;
    private List<VariantAttributeResponseDTO> attributes;
}
