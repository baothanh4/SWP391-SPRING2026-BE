package com.example.SWP391_SPRING2026.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VariantAttributeImageRequestDTO {
    @NotBlank
    private String imageUrl;

    @NotNull
    private Integer sortOrder;
}
