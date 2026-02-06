package com.example.SWP391_SPRING2026.DTO.Request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class VariantAttributeImageListRequestDTO {
    @Valid
    @NotEmpty
    private List<VariantAttributeImageRequestDTO> images;
}
