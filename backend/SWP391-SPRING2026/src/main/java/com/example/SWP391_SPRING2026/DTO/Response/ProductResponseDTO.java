package com.example.SWP391_SPRING2026.DTO.Response;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class ProductResponseDTO {
    private Long id;
    private String name;
    private String brandName;
    private String description;
    private String status;
    private String productImage;

    private List<ProductVariantSummaryDTO> variants;
}
