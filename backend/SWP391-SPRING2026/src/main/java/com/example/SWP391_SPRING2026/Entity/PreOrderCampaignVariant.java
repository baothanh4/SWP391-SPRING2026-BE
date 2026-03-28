package com.example.SWP391_SPRING2026.Entity;

import com.example.SWP391_SPRING2026.Enum.PreOrderPaymentOption;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "campaign_variants")
@IdClass(PreOrderCampaignVariantId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreOrderCampaignVariant {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private PreOrderCampaign campaign;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "deposit_percent", precision = 5, scale = 2)
    private BigDecimal depositPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "preorder_payment_option")
    @Builder.Default
    private PreOrderPaymentOption preorderPaymentOption = PreOrderPaymentOption.FLEXIBLE;
}

