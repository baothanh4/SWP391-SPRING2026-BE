package com.example.SWP391_SPRING2026.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class PreOrderCampaignRequestDTO {
    @NotBlank
    private String name;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull
    private LocalDate fulfillmentDate;

    private Integer preorderLimit;

    @NotNull
    private Boolean isActive;

    @NotEmpty
    private Set<PreOrderCampaignVariantConfigRequestDTO> variantConfigs;
}

