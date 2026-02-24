package com.example.SWP391_SPRING2026.Service;


import com.example.SWP391_SPRING2026.DTO.Request.ComboItemRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Request.CreateComboRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Response.ComboItemResponseDTO;
import com.example.SWP391_SPRING2026.DTO.Response.ProductComboResponseDTO;
import com.example.SWP391_SPRING2026.Entity.ComboItem;
import com.example.SWP391_SPRING2026.Entity.ProductCombo;
import com.example.SWP391_SPRING2026.Entity.ProductVariant;
import com.example.SWP391_SPRING2026.Exception.ResourceNotFoundException;
import com.example.SWP391_SPRING2026.Repository.ProductComboRepository;
import com.example.SWP391_SPRING2026.Repository.ProductVariantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class ProductComboService {
    private final ProductComboRepository productComboRepository;
    private final ProductVariantRepository  productVariantRepository;
    private static final int DISCOUNT_PERCENT = 40;
    @Transactional
    public ProductComboResponseDTO createCombo(CreateComboRequestDTO dto){
        ProductCombo combo =new  ProductCombo();
        combo.setName(dto.getName());
        combo.setDescription(dto.getDescription());
        combo.setActive(true);
        combo.setItems(new ArrayList<>());



        for(ComboItemRequestDTO itemDTO : dto.getItems()){
            ProductVariant variant = productVariantRepository.findById(itemDTO.getVariantId()).orElseThrow(() -> new ResourceNotFoundException("Variant not found : "+itemDTO.getVariantId()));

            int quantity = itemDTO.getQuantity();
            long price = variant.getPrice().longValue();


            ComboItem comboItem = new ComboItem();
            comboItem.setCombo(combo);
            comboItem.setProductVariant(variant);
            comboItem.setQuantity(itemDTO.getQuantity());

            combo.getItems().add(comboItem);
        }
        long comboPrice = calculateComboPrice(dto);
        combo.setComboPrice(comboPrice);

        ProductCombo saved = productComboRepository.save(combo);

        return toDTO(saved);
    }

    @Transactional
    public ProductComboResponseDTO updateCombo(Long comboId,CreateComboRequestDTO dto){
        ProductCombo combo = productComboRepository.findById(comboId).orElseThrow(() -> new ResourceNotFoundException("Combo not found : "+comboId));

        combo.setName(dto.getName());
        combo.setDescription(dto.getDescription());

        combo.getItems().clear();


        for(ComboItemRequestDTO itemDTO : dto.getItems()){
            ProductVariant variant = productVariantRepository.findById(itemDTO.getVariantId()).orElseThrow(()->new ResourceNotFoundException("Variant not found : "+itemDTO.getVariantId()));

            int quantity = itemDTO.getQuantity();
            long price = variant.getPrice().longValue();

            ComboItem comboItem = new ComboItem();
            comboItem.setCombo(combo);
            comboItem.setProductVariant(variant);
            comboItem.setQuantity(itemDTO.getQuantity());

            combo.getItems().add(comboItem);
        }
        long comboPrice = calculateComboPrice(dto);
        combo.setComboPrice(comboPrice);
        return toDTO(productComboRepository.save(combo));
    }

    public Page<ProductComboResponseDTO> getAllActiveCombos(Pageable pageable){
        return productComboRepository.findByActiveTrue(pageable).map(productCombo -> toDTO(productCombo));
    }

    public ProductComboResponseDTO getComboById(Long comboId){
        ProductCombo combo = productComboRepository.findById(comboId).orElseThrow(() -> new ResourceNotFoundException("Combo not found : "+comboId));

        return toDTO(combo);
    }

    @Transactional
    public void deactivateCombo(Long comboId){
        ProductCombo combo = productComboRepository.findById(comboId).orElseThrow(() -> new ResourceNotFoundException("Combo not found: "+ comboId));
        combo.setActive(false);
        productComboRepository.save(combo);
    }

    private ProductComboResponseDTO toDTO(ProductCombo combo){
        ProductComboResponseDTO dto = new ProductComboResponseDTO();
        dto.setId(combo.getId());
        dto.setName(combo.getName());
        dto.setDescription(combo.getDescription());
        dto.setComboPrice(combo.getComboPrice());
        dto.setActive(combo.getActive());

        dto.setItems(
                combo.getItems().stream().map(
                        item ->{
                            ComboItemResponseDTO i = new ComboItemResponseDTO();
                            i.setId(item.getId());
                            i.setProductVariantId(item.getProductVariant().getId());
                            i.setQuantity(item.getQuantity());
                            return i;
                        }
                ).toList()
        );
        return dto;
    }

    private long calculateComboPrice(CreateComboRequestDTO dto) {

        java.math.BigDecimal total = java.math.BigDecimal.ZERO;

        for (ComboItemRequestDTO itemDTO : dto.getItems()) {

            ProductVariant variant = productVariantRepository
                    .findById(itemDTO.getVariantId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Variant not found : "
                                    + itemDTO.getVariantId())
                    );

            java.math.BigDecimal price = variant.getPrice();
            java.math.BigDecimal quantity =
                    java.math.BigDecimal.valueOf(itemDTO.getQuantity());

            total = total.add(price.multiply(quantity));
        }

        java.math.BigDecimal discountMultiplier =
                java.math.BigDecimal.valueOf(100 - DISCOUNT_PERCENT)
                        .divide(java.math.BigDecimal.valueOf(100));

        java.math.BigDecimal finalPrice =
                total.multiply(discountMultiplier);

        return finalPrice.longValue();
    }
}
