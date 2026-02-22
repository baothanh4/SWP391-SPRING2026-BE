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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductComboService {
    private final ProductComboRepository productComboRepository;
    private final ProductVariantRepository  productVariantRepository;

    public ProductCombo createCombo(CreateComboRequestDTO dto){
        ProductCombo combo = new ProductCombo();

        combo.setName(dto.getName());
        combo.setDescription(dto.getDescription());
        combo.setComboPrice(dto.getComboPrice());

        for(ComboItemRequestDTO itemDTO : dto.getItems()){
            ProductVariant variant = productVariantRepository.findById(itemDTO.getVariantId()).orElseThrow(() -> new ResourceNotFoundException("Variant not found: "+ itemDTO.getVariantId()));

            ComboItem comboItem = new ComboItem();
            comboItem.setCombo(combo);
            comboItem.setProductVariant(variant);
            comboItem.setQuantity(itemDTO.getQuantity());

            combo.getItems().add(comboItem);
        }
        return productComboRepository.save(combo);
    }

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
}
