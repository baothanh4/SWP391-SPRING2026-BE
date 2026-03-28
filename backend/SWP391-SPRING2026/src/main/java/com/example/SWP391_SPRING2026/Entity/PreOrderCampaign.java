package com.example.SWP391_SPRING2026.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "preorder_campaigns")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PreOrderCampaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "start_date",nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date",nullable = false)
    private LocalDate endDate;

    @Column(name = "fulfillment_date")
    private LocalDate fulfillmentDate;

    @Column(name = "preorder_limit")
    private Integer preorderLimit;

    @Column(name = "current_preorders", nullable = false)
    private Integer currentPreorders = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PreOrderCampaignVariant> campaignVariants = new ArrayList<>();
}
