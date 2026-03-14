package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Request.ComboItemRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Request.CreateComboRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Response.*;
import com.example.SWP391_SPRING2026.Entity.*;
import com.example.SWP391_SPRING2026.Exception.ResourceNotFoundException;
import com.example.SWP391_SPRING2026.Repository.ProductComboRepository;
import com.example.SWP391_SPRING2026.Repository.ProductVariantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductComboService {

    private final ProductComboRepository productComboRepository;
    private final ProductVariantRepository productVariantRepository;

    // GIỮ 40%
    private static final int DISCOUNT_PERCENT = 40;

    @Transactional
    public ProductComboResponseDTO createCombo(CreateComboRequestDTO dto){

        ProductCombo combo = new ProductCombo();
        combo.setName(dto.getName());
        combo.setDescription(dto.getDescription());
        combo.setImageUrl(dto.getImageUrl());
        combo.setActive(true);

        List<ComboItem> items = new ArrayList<>();

        for(ComboItemRequestDTO itemDTO : dto.getItems()){

            if(itemDTO.getQuantity() <= 0){
                throw new IllegalArgumentException("Quantity must be greater than 0");
            }

            ProductVariant variant = productVariantRepository.findById(itemDTO.getVariantId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Variant not found: " + itemDTO.getVariantId()
                            )
                    );

            ComboItem comboItem = new ComboItem();
            comboItem.setCombo(combo);
            comboItem.setProductVariant(variant);
            comboItem.setQuantity(itemDTO.getQuantity());

            items.add(comboItem);
        }

        combo.setItems(items);

        long price = calculateComboPrice(dto);

        combo.setComboPrice(price);

        return toDTO(productComboRepository.save(combo));
    }

    @Transactional
    public ProductComboResponseDTO updateCombo(Long comboId, CreateComboRequestDTO dto){

        ProductCombo combo = productComboRepository.findById(comboId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Combo not found: " + comboId)
                );

        combo.setName(dto.getName());
        combo.setDescription(dto.getDescription());
        combo.setImageUrl(dto.getImageUrl());

        combo.getItems().clear();

        for(ComboItemRequestDTO itemDTO : dto.getItems()){

            if(itemDTO.getQuantity() <= 0){
                throw new IllegalArgumentException("Quantity must be greater than 0");
            }

            ProductVariant variant = productVariantRepository
                    .findById(itemDTO.getVariantId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Variant not found: " + itemDTO.getVariantId()
                            )
                    );

            ComboItem comboItem = new ComboItem();
            comboItem.setCombo(combo);
            comboItem.setProductVariant(variant);
            comboItem.setQuantity(itemDTO.getQuantity());

            combo.getItems().add(comboItem);
        }

        combo.setComboPrice(calculateComboPrice(dto));

        productComboRepository.save(combo);

        return toDTO(combo);
    }

    public Page<ProductComboResponseDTO> getAllActiveCombos(Pageable pageable){

        return productComboRepository
                .findByActiveTrue(pageable)
                .map(this::toDTO);
    }

    public ProductComboResponseDTO getComboById(Long comboId){

        ProductCombo combo = productComboRepository.findById(comboId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Combo not found : " + comboId)
                );

        return toDTO(combo);
    }

    @Transactional
    public void deactivateCombo(Long comboId){

        ProductCombo combo = productComboRepository.findById(comboId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Combo not found: " + comboId)
                );

        combo.setActive(false);

        productComboRepository.save(combo);
    }

    private ProductComboResponseDTO toDTO(ProductCombo combo){

        ProductComboResponseDTO dto = new ProductComboResponseDTO();

        dto.setId(combo.getId());
        dto.setName(combo.getName());
        dto.setDescription(combo.getDescription());
        dto.setComboPrice(combo.getComboPrice());
        dto.setImageUrl(combo.getImageUrl());
        dto.setActive(combo.getActive());

        List<ComboItemResponseDTO> itemDTOs = new ArrayList<>();

        for(ComboItem item : combo.getItems()){

            ProductVariant variant = item.getProductVariant();

            ComboItemResponseDTO response = new ComboItemResponseDTO();

            response.setId(item.getId());
            response.setProductVariantId(variant.getId());
            response.setQuantity(item.getQuantity());

            List<VariantAttributeResponseDTO> attrs = new ArrayList<>();

            for(VariantAttribute attr : variant.getAttributes()){

                VariantAttributeResponseDTO adto =
                        new VariantAttributeResponseDTO();

                adto.setId(attr.getId());
                adto.setAttributeName(attr.getAttributeName());
                adto.setAttributeValue(attr.getAttributeValue());

                List<VariantAttributeImageResponseDTO> images =
                        attr.getImages()
                                .stream()
                                .sorted(Comparator.comparing(
                                        VariantAttributeImage::getSortOrder))
                                .map(img ->
                                        new VariantAttributeImageResponseDTO(
                                                img.getId(),
                                                img.getImageUrl(),
                                                img.getSortOrder(),
                                                attr.getId()
                                        )
                                )
                                .toList();

                adto.setImages(images);

                attrs.add(adto);
            }

            response.setAttributes(attrs);

            itemDTOs.add(response);
        }

        dto.setItems(itemDTOs);

        return dto;
    }

    private long calculateComboPrice(CreateComboRequestDTO dto){

        BigDecimal total = BigDecimal.ZERO;

        for(ComboItemRequestDTO itemDTO : dto.getItems()){

            ProductVariant variant = productVariantRepository
                    .findById(itemDTO.getVariantId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Variant not found : " + itemDTO.getVariantId()
                            )
                    );

            BigDecimal price = variant.getPrice();
            BigDecimal quantity = BigDecimal.valueOf(itemDTO.getQuantity());

            total = total.add(price.multiply(quantity));
        }

        BigDecimal discountMultiplier =
                BigDecimal.valueOf(100 - DISCOUNT_PERCENT)
                        .divide(BigDecimal.valueOf(100));

        BigDecimal finalPrice = total
                .multiply(discountMultiplier)
                .setScale(0, RoundingMode.HALF_UP);

        return finalPrice.longValue();
    }
}