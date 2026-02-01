package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Request.ProductRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Request.VariantAttributeRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Response.ProductDetailResponseDTO;
import com.example.SWP391_SPRING2026.DTO.Response.ProductResponseDTO;
import com.example.SWP391_SPRING2026.DTO.Response.ProductVariantDetailDTO;
import com.example.SWP391_SPRING2026.Entity.Product;
import com.example.SWP391_SPRING2026.Enum.ProductStatus;
import com.example.SWP391_SPRING2026.Repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    public ProductResponseDTO createProduct(ProductRequestDTO dto){
        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setBrandName(dto.getBrandName());
        product.setProductImage(dto.getProductImage());
        product.setStatus(ProductStatus.ACTIVE);

        productRepository.save(product);
        return toDTO(product);
    }

    public List<ProductResponseDTO> getProducts(){
        return productRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public ProductDetailResponseDTO getProductDetails(Long productId){
        Product product = productRepository.findById(productId).orElseThrow(() ->new RuntimeException("Product not found"));

        ProductDetailResponseDTO dto = new ProductDetailResponseDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setBrandName(product.getBrandName());
        dto.setDescription(product.getDescription());
        dto.setProductImage(product.getProductImage());

        List<ProductVariantDetailDTO> variants = product.getVariants().stream()
                .map(variant -> {
                    ProductVariantDetailDTO dto2 = new ProductVariantDetailDTO();
                    dto2.setId(variant.getId());
                    dto2.setSku(variant.getSku());
                    dto2.setPrice(variant.getPrice());
                    dto2.setStockQuantity(variant.getStockQuantity());

                    List<VariantAttributeRequestDTO> attrs= variant.getAttributes().stream()
                            .map(attr -> {
                                VariantAttributeRequestDTO dto3 = new VariantAttributeRequestDTO();
                                dto3.setAttributeName(attr.getAttributeName());
                                dto3.setAttributeValue(attr.getAttributeValue());
                                return dto3;
                            }).toList();
                    dto2.setVariantAttributeRequestDTOList(attrs);
                    return dto2;
                }).toList();
        dto.setVariants(variants);
        return dto;
    }

    public ProductResponseDTO updateProduct(Long productId,ProductRequestDTO dto){
        Product product = productRepository.findById(productId).orElseThrow(() ->new RuntimeException("Product not found"));

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setBrandName(dto.getBrandName());
        product.setProductImage(dto.getProductImage());

        productRepository.save(product);
        return toDTO(product);
    }

    public void deactivateProduct(Long productId){
        Product product = productRepository.findById(productId).orElseThrow(() ->new RuntimeException("Product not found"));

        product.setStatus(ProductStatus.INACTIVE);
        productRepository.save(product);
    }

    private ProductResponseDTO toDTO(Product product){
        ProductResponseDTO dto = new ProductResponseDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setBrandName(product.getBrandName());
        dto.setStatus(product.getStatus().name());
        return dto;
    }


}
