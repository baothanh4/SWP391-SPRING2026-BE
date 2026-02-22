package com.example.SWP391_SPRING2026.DTO.Request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateComboRequestDTO {
    @NotBlank
    private String name;

    private String description;

    @NotNull
    @Min(0)
    private Long comboPrice;

    @NotEmpty
    private List<ComboItemRequestDTO> items;
}
