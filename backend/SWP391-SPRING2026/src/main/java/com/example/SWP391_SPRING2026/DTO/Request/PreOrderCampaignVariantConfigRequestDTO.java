package com.example.SWP391_SPRING2026.DTO.Request;

import com.example.SWP391_SPRING2026.Enum.PreOrderPaymentOption;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PreOrderCampaignVariantConfigRequestDTO {

    @NotNull
    private Long variantId;

    @DecimalMin(value = "0.00")
    @DecimalMax(value = "100.00")
    private BigDecimal depositPercent;

    @NotNull
    private PreOrderPaymentOption preorderPaymentOption;
}

