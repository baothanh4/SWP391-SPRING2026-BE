package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Request.ProductVariantRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Response.ProductVariantResponseDTO;
import com.example.SWP391_SPRING2026.Entity.Product;
import com.example.SWP391_SPRING2026.Entity.ProductVariant;
import com.example.SWP391_SPRING2026.Repository.ProductRepository;
import com.example.SWP391_SPRING2026.Repository.ProductVariantRepository;
import com.example.SWP391_SPRING2026.Utility.VariantAvailabilityResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    public ProductVariantResponseDTO create(Long productId, ProductVariantRequestDTO dto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product Not Found"));

        ProductVariant productVariant = new ProductVariant();
        productVariant.setSku(dto.getSku());
        productVariant.setPrice(dto.getPrice());
        productVariant.setStockQuantity(dto.getStockQuantity());
        productVariant.setSaleType(dto.getSaleType());

        productVariant.setAllowPreorder(Boolean.TRUE.equals(dto.getAllowPreorder()));
        productVariant.setPreorderLimit(dto.getPreorderLimit());
        productVariant.setCurrentPreorders(0);
        productVariant.setPreorderFulfillmentDate(dto.getPreorderFulfillmentDate());

        productVariant.setProduct(product);
        productVariantRepository.save(productVariant);

        return toDTO(productVariant);
    }

    public ProductVariantResponseDTO update(Long variantId, ProductVariantRequestDTO dto) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Product Variant Not Found"));

        variant.setSku(dto.getSku());
        variant.setPrice(dto.getPrice());
        variant.setStockQuantity(dto.getStockQuantity());
        variant.setSaleType(dto.getSaleType());

        variant.setAllowPreorder(Boolean.TRUE.equals(dto.getAllowPreorder()));
        variant.setPreorderLimit(dto.getPreorderLimit());
        variant.setPreorderFulfillmentDate(dto.getPreorderFulfillmentDate());

        productVariantRepository.save(variant);
        return toDTO(variant);
    }

    public List<ProductVariantResponseDTO> getAllVariants() {
        List<ProductVariant> productVariants = productVariantRepository.findAll();

        return productVariants.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public void delete(Long variantId) {
        productVariantRepository.deleteById(variantId);
    }

    private ProductVariantResponseDTO toDTO(ProductVariant productVariant) {
        return ProductVariantResponseDTO.builder()
                .id(productVariant.getId())
                .sku(productVariant.getSku())
                .price(productVariant.getPrice())
                .stockQuantity(productVariant.getStockQuantity())
                .saleType(productVariant.getSaleType())
                .allowPreorder(productVariant.getAllowPreorder())
                .preorderLimit(productVariant.getPreorderLimit())
                .currentPreorders(productVariant.getCurrentPreorders())
                .preorderFulfillmentDate(productVariant.getPreorderFulfillmentDate())
                .availabilityStatus(VariantAvailabilityResolver.resolve(productVariant))
                .product_id(productVariant.getProduct().getId())
                .build();
    }
}