package com.example.SWP391_SPRING2026.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComboItemResponseDTO {
    private Long id;
    private Long productVariantId;
    private Integer quantity;

    private List<VariantAttributeResponseDTO> attributes;
}
