package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Request.VariantAttributeImageRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Response.VariantAttributeImageResponseDTO;
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

    private final VariantAttributeRepository variantAttributeRepository;
    private final VariantAttributeImageRepository variantAttributeImageRepository;

    @Transactional
    public List<VariantAttributeImageResponseDTO> addImages(
            Long attributeId,
            List<VariantAttributeImageRequestDTO> images){

        VariantAttribute attribute = variantAttributeRepository.findById(attributeId)
                .orElseThrow(() -> new RuntimeException("Attribute not found"));

        if(images == null || images.isEmpty()){
            return List.of();
        }

        return images.stream().map(dto -> {

            VariantAttributeImage image = new VariantAttributeImage();

            image.setImageUrl(dto.getImageUrl());
            image.setSortOrder(dto.getSortOrder());
            image.setVariantAttribute(attribute);

            VariantAttributeImage saved = variantAttributeImageRepository.save(image);

            return new VariantAttributeImageResponseDTO(
                    saved.getId(),
                    saved.getImageUrl(),
                    saved.getSortOrder(),
                    attributeId
            );

        }).toList();
    }

    @Transactional
    public void updateImage(Long imageId, VariantAttributeImageRequestDTO dto){

        VariantAttributeImage image = variantAttributeImageRepository
                .findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        image.setImageUrl(dto.getImageUrl());
        image.setSortOrder(dto.getSortOrder());

        variantAttributeImageRepository.save(image);
    }

    public void deleteImage(Long imageId){
        variantAttributeImageRepository.deleteById(imageId);
    }
}