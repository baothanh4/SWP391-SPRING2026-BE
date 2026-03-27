package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Request.PreOrderCampaignRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Response.PreOrderCampaignResponseDTO;
import com.example.SWP391_SPRING2026.Entity.PreOrderCampaign;
import com.example.SWP391_SPRING2026.Entity.ProductVariant;
import com.example.SWP391_SPRING2026.Enum.SaleType;
import com.example.SWP391_SPRING2026.Exception.BadRequestException;
import com.example.SWP391_SPRING2026.Exception.ResourceNotFoundException;
import com.example.SWP391_SPRING2026.Repository.PreOrderCampaignRepository;
import com.example.SWP391_SPRING2026.Repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PreOrderCampaignService {

    private final PreOrderCampaignRepository preOrderCampaignRepository;
    private final ProductVariantRepository productVariantRepository;

    public PreOrderCampaignResponseDTO create(PreOrderCampaignRequestDTO dto) {
        validateRequest(dto);

        List<Long> variantIds = dto.getVariantIds().stream().toList();
        validateNoOverlaps(variantIds, dto.getStartDate(), dto.getEndDate(), null, dto.getIsActive());

        Set<ProductVariant> variants = fetchVariants(dto.getVariantIds());

        PreOrderCampaign campaign = PreOrderCampaign.builder()
                .name(dto.getName().trim())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .fulfillmentDate(dto.getFulfillmentDate())
                .preorderLimit(dto.getPreorderLimit())
                .isActive(dto.getIsActive())
                .currentPreorders(0)
                .variants(variants)
                .build();

        PreOrderCampaign saved = preOrderCampaignRepository.save(campaign);
        refreshVariantCampaignState(extractVariantIds(variants));

        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<PreOrderCampaignResponseDTO> getAll() {
        return preOrderCampaignRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public PreOrderCampaignResponseDTO getById(Long campaignId) {
        return toDTO(getCampaignOrThrow(campaignId));
    }

    public PreOrderCampaignResponseDTO update(Long campaignId, PreOrderCampaignRequestDTO dto) {
        validateRequest(dto);

        PreOrderCampaign campaign = getCampaignOrThrow(campaignId);
        Set<Long> oldVariantIds = extractVariantIds(campaign.getVariants());

        List<Long> newVariantIds = dto.getVariantIds().stream().toList();
        validateNoOverlaps(newVariantIds, dto.getStartDate(), dto.getEndDate(), campaignId, dto.getIsActive());

        Set<ProductVariant> variants = fetchVariants(dto.getVariantIds());

        campaign.setName(dto.getName().trim());
        campaign.setStartDate(dto.getStartDate());
        campaign.setEndDate(dto.getEndDate());
        campaign.setFulfillmentDate(dto.getFulfillmentDate());
        campaign.setPreorderLimit(dto.getPreorderLimit());
        campaign.setIsActive(dto.getIsActive());
        campaign.setVariants(variants);

        PreOrderCampaign saved = preOrderCampaignRepository.save(campaign);

        Set<Long> impactedVariantIds = new LinkedHashSet<>(oldVariantIds);
        impactedVariantIds.addAll(extractVariantIds(variants));
        refreshVariantCampaignState(impactedVariantIds);

        return toDTO(saved);
    }

    public void delete(Long campaignId) {
        PreOrderCampaign campaign = getCampaignOrThrow(campaignId);
        Set<Long> impactedVariantIds = extractVariantIds(campaign.getVariants());

        preOrderCampaignRepository.delete(campaign);
        refreshVariantCampaignState(impactedVariantIds);
    }

    public PreOrderCampaignResponseDTO activate(Long campaignId) {
        PreOrderCampaign campaign = getCampaignOrThrow(campaignId);

        validateNoOverlaps(
                campaign.getVariants().stream().map(ProductVariant::getId).toList(),
                campaign.getStartDate(),
                campaign.getEndDate(),
                campaignId,
                true
        );

        campaign.setIsActive(true);
        PreOrderCampaign saved = preOrderCampaignRepository.save(campaign);
        refreshVariantCampaignState(extractVariantIds(campaign.getVariants()));
        return toDTO(saved);
    }

    public PreOrderCampaignResponseDTO deactivate(Long campaignId) {
        PreOrderCampaign campaign = getCampaignOrThrow(campaignId);
        campaign.setIsActive(false);

        PreOrderCampaign saved = preOrderCampaignRepository.save(campaign);
        refreshVariantCampaignState(extractVariantIds(campaign.getVariants()));
        return toDTO(saved);
    }

    private void validateRequest(PreOrderCampaignRequestDTO dto) {
        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new BadRequestException("startDate must be before or equal to endDate");
        }

        if (dto.getFulfillmentDate().isBefore(dto.getEndDate())) {
            throw new BadRequestException("fulfillmentDate must be on or after endDate");
        }

        if (dto.getPreorderLimit() != null && dto.getPreorderLimit() < 0) {
            throw new BadRequestException("preorderLimit must be >= 0 or null");
        }
    }

    private void validateNoOverlaps(
            List<Long> variantIds,
            LocalDate startDate,
            LocalDate endDate,
            Long campaignId,
            boolean isActive
    ) {
        if (!isActive) {
            return;
        }

        List<PreOrderCampaign> overlaps = (campaignId == null)
                ? preOrderCampaignRepository.findActiveOverlappingCampaigns(variantIds, startDate, endDate)
                : preOrderCampaignRepository.findActiveOverlappingCampaignsExcludingId(campaignId, variantIds, startDate, endDate);

        if (!overlaps.isEmpty()) {
            throw new BadRequestException("Another active campaign is overlapping on one or more variants");
        }
    }

    private Set<ProductVariant> fetchVariants(Set<Long> variantIds) {
        List<ProductVariant> variants = productVariantRepository.findAllById(variantIds);

        if (variants.size() != variantIds.size()) {
            throw new ResourceNotFoundException("One or more product variants were not found");
        }

        return new LinkedHashSet<>(variants);
    }

    private void refreshVariantCampaignState(Set<Long> variantIds) {
        if (variantIds == null || variantIds.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();

        for (Long variantId : variantIds) {
            ProductVariant variant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Variant not found: " + variantId));

            List<PreOrderCampaign> activeCampaigns = preOrderCampaignRepository
                    .findActiveCampaignsForVariant(variantId, today);

            if (activeCampaigns.isEmpty()) {
                variant.setAllowPreorder(false);
                variant.setPreorderStartDate(null);
                variant.setPreorderEndDate(null);
                variant.setPreorderFulfillmentDate(null);
                variant.setPreorderLimit(null);

                if (variant.getSaleType() == SaleType.PRE_ORDER) {
                    variant.setSaleType(SaleType.IN_STOCK);
                }
                continue;
            }

            PreOrderCampaign campaign = activeCampaigns.get(0);
            variant.setSaleType(SaleType.PRE_ORDER);
            variant.setAllowPreorder(true);
            variant.setPreorderStartDate(campaign.getStartDate());
            variant.setPreorderEndDate(campaign.getEndDate());
            variant.setPreorderFulfillmentDate(campaign.getFulfillmentDate());
            variant.setPreorderLimit(campaign.getPreorderLimit());
        }
    }

    private PreOrderCampaign getCampaignOrThrow(Long campaignId) {
        return preOrderCampaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Preorder campaign not found: " + campaignId));
    }

    private Set<Long> extractVariantIds(Set<ProductVariant> variants) {
        return variants.stream()
                .map(ProductVariant::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private PreOrderCampaignResponseDTO toDTO(PreOrderCampaign campaign) {
        return PreOrderCampaignResponseDTO.builder()
                .id(campaign.getId())
                .name(campaign.getName())
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .fulfillmentDate(campaign.getFulfillmentDate())
                .preorderLimit(campaign.getPreorderLimit())
                .currentPreorders(campaign.getCurrentPreorders())
                .isActive(campaign.getIsActive())
                .variantIds(campaign.getVariants().stream().map(ProductVariant::getId).collect(Collectors.toSet()))
                .build();
    }
}

