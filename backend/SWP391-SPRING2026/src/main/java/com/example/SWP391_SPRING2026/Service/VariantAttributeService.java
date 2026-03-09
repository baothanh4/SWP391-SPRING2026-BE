package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Request.VariantAttributeRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Response.VariantAttributeResponseSimpleDTO;
import com.example.SWP391_SPRING2026.Entity.ProductVariant;
import com.example.SWP391_SPRING2026.Entity.VariantAttribute;
import com.example.SWP391_SPRING2026.Repository.ProductVariantRepository;
import com.example.SWP391_SPRING2026.Repository.VariantAttributeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VariantAttributeService {

    private final ProductVariantRepository productVariantRepository;
    private final VariantAttributeRepository variantAttributeRepository;

    public VariantAttributeResponseSimpleDTO addAttribute(Long variantId, VariantAttributeRequestDTO dto){

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Variant not found"));

        VariantAttribute attribute = new VariantAttribute();
        attribute.setProductVariant(variant);
        attribute.setAttributeName(dto.getAttributeName());
        attribute.setAttributeValue(dto.getAttributeValue());

        VariantAttribute saved = variantAttributeRepository.save(attribute);

        return new VariantAttributeResponseSimpleDTO(
                saved.getId(),
                saved.getAttributeName(),
                saved.getAttributeValue(),
                saved.getProductVariant().getId()
        );
    }

    public void updateAttribute(Long attributeId, VariantAttributeRequestDTO dto){

        VariantAttribute attribute = variantAttributeRepository.findById(attributeId)
                .orElseThrow(() -> new RuntimeException("Attribute not found"));

        attribute.setAttributeName(dto.getAttributeName());
        attribute.setAttributeValue(dto.getAttributeValue());

        variantAttributeRepository.save(attribute);
    }

    public void deleteAttribute(Long attributeId){
        variantAttributeRepository.deleteById(attributeId);
    }
}