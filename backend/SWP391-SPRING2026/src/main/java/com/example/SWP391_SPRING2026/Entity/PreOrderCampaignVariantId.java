package com.example.SWP391_SPRING2026.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreOrderCampaignVariantId implements Serializable {
    private Long campaign;
    private Long variant;
}

