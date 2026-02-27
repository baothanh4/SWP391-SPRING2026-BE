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
public class ProductComboResponseDTO {
    private Long id;
    private String name;
    private String description;
    private Long comboPrice;
    private Boolean active;
    private List<ComboItemResponseDTO> items;
}
