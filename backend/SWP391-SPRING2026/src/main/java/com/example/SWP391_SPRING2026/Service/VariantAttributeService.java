package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Request.VariantAttributeRequestDTO;
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

    public void addAttribute(Long variantId, VariantAttributeRequestDTO dto){
        ProductVariant productVariant = productVariantRepository.findById(variantId).orElseThrow(()-> new RuntimeException("Variant not found"));

        VariantAttribute attribute = new VariantAttribute();
        attribute.setAttributeName(dto.getAttributeName());
        attribute.setAttributeValue(dto.getAttributeValue());
        attribute.setProductVariant(productVariant);
        variantAttributeRepository.save(attribute);
    }

    public void updateAttribute(Long attributeId,VariantAttributeRequestDTO dto){
        VariantAttribute attribute = variantAttributeRepository.findById(attributeId).orElseThrow(()-> new RuntimeException("Attribute not found"));

        attribute.setAttributeValue(dto.getAttributeValue());
        attribute.setAttributeName(dto.getAttributeName());
        variantAttributeRepository.save(attribute);
    }

    public void deleteAttribute(Long attributeId){
        variantAttributeRepository.deleteById(attributeId);
    }
}
