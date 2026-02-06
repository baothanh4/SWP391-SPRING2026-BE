package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Request.VariantAttributeImageRequestDTO;
import com.example.SWP391_SPRING2026.Entity.VariantAttribute;
import com.example.SWP391_SPRING2026.Entity.VariantAttributeImage;
import com.example.SWP391_SPRING2026.Repository.VariantAttributeImageRepository;
import com.example.SWP391_SPRING2026.Repository.VariantAttributeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VariantAttributeImageService {
    private final VariantAttributeRepository  variantAttributeRepository;
    private final VariantAttributeImageRepository variantAttributeImageRepository;

    @Transactional
    public void addImages(Long attributeId, List<VariantAttributeImageRequestDTO> images){
        VariantAttribute attribute = variantAttributeRepository.findById(attributeId).orElseThrow(() -> new RuntimeException("Attribute not found"));

        for(VariantAttributeImageRequestDTO dto : images){
            VariantAttributeImage image = new VariantAttributeImage();
            image.setImageUrl(dto.getImageUrl());
            image.setSortOrder(dto.getSortOrder());
            image.setVariantAttribute(attribute);
            variantAttributeImageRepository.save(image);
        }
    }

    public void deleteImage(Long imageId){
        variantAttributeImageRepository.deleteById(imageId);
    }
}
