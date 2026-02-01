package com.example.SWP391_SPRING2026.DTO.Response;

import lombok.Data;

import java.util.List;

@Data
public class ProductDetailResponseDTO {
    private Long id;
    private String name;
    private String brandName;
    private String description;
    private String productImage;
    private List<ProductVariantDetailDTO> variants;
}
