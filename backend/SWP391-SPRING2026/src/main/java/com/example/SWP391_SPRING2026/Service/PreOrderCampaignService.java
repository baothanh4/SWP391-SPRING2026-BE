package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Request.PreOrderCampaignVariantConfigRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Request.PreOrderCampaignRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Response.PreOrderCampaignVariantConfigResponseDTO;
import com.example.SWP391_SPRING2026.DTO.Response.PreOrderCampaignResponseDTO;
import com.example.SWP391_SPRING2026.Entity.PreOrderCampaign;
import com.example.SWP391_SPRING2026.Entity.PreOrderCampaignVariant;
import com.example.SWP391_SPRING2026.Entity.ProductVariant;
import com.example.SWP391_SPRING2026.Enum.PreOrderPaymentOption;
import com.example.SWP391_SPRING2026.Enum.SaleType;
import com.example.SWP391_SPRING2026.Exception.BadRequestException;
import com.example.SWP391_SPRING2026.Exception.ResourceNotFoundException;
import com.example.SWP391_SPRING2026.Repository.PreOrderCampaignRepository;
import com.example.SWP391_SPRING2026.Repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

        List<VariantConfigInput> configs = resolveConfigInputs(dto);
        List<Long> variantIds = configs.stream().map(VariantConfigInput::variantId).toList();
        validateNoOverlaps(variantIds, dto.getStartDate(), dto.getEndDate(), null, dto.getIsActive());

        Map<Long, ProductVariant> variantMap = fetchVariantsAsMap(new LinkedHashSet<>(variantIds));

        PreOrderCampaign campaign = PreOrderCampaign.builder()
                .name(dto.getName().trim())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .fulfillmentDate(dto.getFulfillmentDate())
                .preorderLimit(dto.getPreorderLimit())
                .isActive(dto.getIsActive())
                .currentPreorders(0)
                .build();

        attachCampaignVariants(campaign, configs, variantMap);

        PreOrderCampaign saved = preOrderCampaignRepository.save(campaign);
        refreshVariantCampaignState(new LinkedHashSet<>(variantIds));

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
        Set<Long> oldVariantIds = extractVariantIds(campaign.getCampaignVariants());

        List<VariantConfigInput> configs = resolveConfigInputs(dto);
        List<Long> newVariantIds = configs.stream().map(VariantConfigInput::variantId).toList();
        validateNoOverlaps(newVariantIds, dto.getStartDate(), dto.getEndDate(), campaignId, dto.getIsActive());

        Map<Long, ProductVariant> variantMap = fetchVariantsAsMap(new LinkedHashSet<>(newVariantIds));

        campaign.setName(dto.getName().trim());
        campaign.setStartDate(dto.getStartDate());
        campaign.setEndDate(dto.getEndDate());
        campaign.setFulfillmentDate(dto.getFulfillmentDate());
        campaign.setPreorderLimit(dto.getPreorderLimit());
        campaign.setIsActive(dto.getIsActive());

        attachCampaignVariants(campaign, configs, variantMap);

        PreOrderCampaign saved = preOrderCampaignRepository.save(campaign);

        Set<Long> impactedVariantIds = new LinkedHashSet<>(oldVariantIds);
        impactedVariantIds.addAll(newVariantIds);
        refreshVariantCampaignState(impactedVariantIds);

        return toDTO(saved);
    }

    public void delete(Long campaignId) {
        PreOrderCampaign campaign = getCampaignOrThrow(campaignId);
        Set<Long> impactedVariantIds = extractVariantIds(campaign.getCampaignVariants());

        preOrderCampaignRepository.delete(campaign);
        refreshVariantCampaignState(impactedVariantIds);
    }

    public PreOrderCampaignResponseDTO activate(Long campaignId) {
        PreOrderCampaign campaign = getCampaignOrThrow(campaignId);

        validateNoOverlaps(
                campaign.getCampaignVariants().stream().map(cv -> cv.getVariant().getId()).toList(),
                campaign.getStartDate(),
                campaign.getEndDate(),
                campaignId,
                true
        );

        campaign.setIsActive(true);
        PreOrderCampaign saved = preOrderCampaignRepository.save(campaign);
        refreshVariantCampaignState(extractVariantIds(campaign.getCampaignVariants()));
        return toDTO(saved);
    }

    public PreOrderCampaignResponseDTO deactivate(Long campaignId) {
        PreOrderCampaign campaign = getCampaignOrThrow(campaignId);
        campaign.setIsActive(false);

        PreOrderCampaign saved = preOrderCampaignRepository.save(campaign);
        refreshVariantCampaignState(extractVariantIds(campaign.getCampaignVariants()));
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

        if (dto.getVariantConfigs() == null || dto.getVariantConfigs().isEmpty()) {
            throw new BadRequestException("variantConfigs is required");
        }
    }

    private List<VariantConfigInput> resolveConfigInputs(PreOrderCampaignRequestDTO dto) {
        List<VariantConfigInput> configs = dto.getVariantConfigs().stream()
                .map(this::toValidatedConfigInput)
                .toList();

        Set<Long> uniqueIds = configs.stream()
                .map(VariantConfigInput::variantId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (uniqueIds.size() != configs.size()) {
            throw new BadRequestException("Duplicate variantId found in variantConfigs");
        }

        return configs;
    }

    private VariantConfigInput toValidatedConfigInput(PreOrderCampaignVariantConfigRequestDTO config) {
        if (config.getVariantId() == null) {
            throw new BadRequestException("variantId is required in variantConfigs");
        }

        if (config.getPreorderPaymentOption() == null) {
            throw new BadRequestException("preorderPaymentOption is required in variantConfigs");
        }

        PreOrderPaymentOption option = config.getPreorderPaymentOption();
        BigDecimal depositPercent = normalizePercent(config.getDepositPercent());

        if (option == PreOrderPaymentOption.FULL_ONLY) {
            return new VariantConfigInput(config.getVariantId(), null, option);
        }

        if (depositPercent == null) {
            throw new BadRequestException("depositPercent is required for DEPOSIT_ONLY or FLEXIBLE option");
        }

        if (depositPercent.compareTo(BigDecimal.ZERO) <= 0 || depositPercent.compareTo(new BigDecimal("100")) >= 0) {
            throw new BadRequestException("depositPercent must be > 0 and < 100");
        }

        return new VariantConfigInput(config.getVariantId(), depositPercent, option);
    }

    private BigDecimal normalizePercent(BigDecimal percent) {
        if (percent == null) {
            return null;
        }
        return percent.setScale(2, RoundingMode.HALF_UP);
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

    private Map<Long, ProductVariant> fetchVariantsAsMap(Set<Long> variantIds) {
        List<ProductVariant> variants = productVariantRepository.findAllById(variantIds);

        if (variants.size() != variantIds.size()) {
            throw new ResourceNotFoundException("One or more product variants were not found");
        }

        return variants.stream().collect(Collectors.toMap(
                ProductVariant::getId,
                v -> v,
                (a, b) -> a,
                LinkedHashMap::new
        ));
    }

    private void attachCampaignVariants(
            PreOrderCampaign campaign,
            List<VariantConfigInput> configs,
            Map<Long, ProductVariant> variantMap
    ) {
        campaign.getCampaignVariants().clear();

        for (VariantConfigInput config : configs) {
            ProductVariant variant = variantMap.get(config.variantId());

            PreOrderCampaignVariant link = PreOrderCampaignVariant.builder()
                    .campaign(campaign)
                    .variant(variant)
                    .depositPercent(config.depositPercent())
                    .preorderPaymentOption(config.preorderPaymentOption())
                    .build();

            campaign.getCampaignVariants().add(link);
        }
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

    private Set<Long> extractVariantIds(Collection<PreOrderCampaignVariant> campaignVariants) {
        return campaignVariants.stream()
                .map(link -> link.getVariant().getId())
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
                .variantIds(campaign.getCampaignVariants().stream()
                        .map(link -> link.getVariant().getId())
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .variantConfigs(campaign.getCampaignVariants().stream()
                        .map(link -> PreOrderCampaignVariantConfigResponseDTO.builder()
                                .variantId(link.getVariant().getId())
                                .depositPercent(link.getDepositPercent())
                                .preorderPaymentOption(link.getPreorderPaymentOption())
                                .build())
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .build();
    }

    private record VariantConfigInput(
            Long variantId,
            BigDecimal depositPercent,
            PreOrderPaymentOption preorderPaymentOption
    ) {
    }
}

