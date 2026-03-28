package com.example.SWP391_SPRING2026.DTO.Response;

import com.example.SWP391_SPRING2026.Enum.PreOrderPaymentOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreOrderCampaignVariantConfigResponseDTO {
    private Long variantId;
    private BigDecimal depositPercent;
    private PreOrderPaymentOption preorderPaymentOption;
}

