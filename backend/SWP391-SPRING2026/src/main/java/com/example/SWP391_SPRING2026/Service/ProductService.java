package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Request.ProductRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Request.VariantAttributeRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Response.ProductDetailResponseDTO;
import com.example.SWP391_SPRING2026.DTO.Response.ProductResponseDTO;
import com.example.SWP391_SPRING2026.DTO.Response.ProductSearchItemDTO;
import com.example.SWP391_SPRING2026.DTO.Response.ProductVariantDetailDTO;
import com.example.SWP391_SPRING2026.Entity.Product;
import com.example.SWP391_SPRING2026.Enum.ProductStatus;
import com.example.SWP391_SPRING2026.Exception.BadRequestException;
import com.example.SWP391_SPRING2026.Exception.ResourceNotFoundException;
import com.example.SWP391_SPRING2026.Repository.ProductRepository;
import com.example.SWP391_SPRING2026.Repository.Projection.ProductSearchProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    public Page<ProductSearchItemDTO> searchPublicProducts(
            String keyword,
            String brand,
            ProductStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock,
            Pageable pageable
    ) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("minPrice must be <= maxPrice");
        }

        String kw = normalize(keyword);
        String br = normalize(brand);

        return productRepository.searchPublicProducts(kw, br, status, minPrice, maxPrice, inStock, pageable)
                .map(this::toSearchItemDTO);
    }

    private ProductSearchItemDTO toSearchItemDTO(ProductSearchProjection p) {
        ProductSearchItemDTO dto = new ProductSearchItemDTO();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setBrandName(p.getBrandName());
        dto.setStatus(p.getStatus() == null ? null : p.getStatus().name());
        dto.setProductImage(p.getProductImage());

        dto.setMinPrice(p.getMinPrice());
        dto.setMaxPrice(p.getMaxPrice());

        Long stock = p.getTotalStock() == null ? 0L : p.getTotalStock();
        dto.setTotalStock(stock);
        dto.setHasStock(stock > 0);

        return dto;
    }

    private String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
    @Transactional(readOnly = true)
    public ProductDetailResponseDTO getPublicProductDetails(Long productId) {
        Product product = productRepository.findDetailById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        // customer chỉ được xem ACTIVE
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new ResourceNotFoundException("Product not found");
        }

        return mapToDetailDTO(product);
    }
    private ProductDetailResponseDTO mapToDetailDTO(Product product) {
        ProductDetailResponseDTO dto = new ProductDetailResponseDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setBrandName(product.getBrandName());
        dto.setDescription(product.getDescription());
        dto.setProductImage(product.getProductImage());

        List<ProductVariantDetailDTO> variants = product.getVariants().stream()
                .map(v -> {
                    ProductVariantDetailDTO vdto = new ProductVariantDetailDTO();
                    vdto.setId(v.getId());
                    vdto.setSku(v.getSku());
                    vdto.setPrice(v.getPrice());
                    vdto.setStockQuantity(v.getStockQuantity());

                    List<VariantAttributeRequestDTO> attrs = v.getAttributes().stream()
                            .map(a -> {
                                VariantAttributeRequestDTO adto = new VariantAttributeRequestDTO();
                                adto.setAttributeName(a.getAttributeName());
                                adto.setAttributeValue(a.getAttributeValue());
                                return adto;
                            })
                            .toList();

                    vdto.setVariantAttributeRequestDTOList(attrs);
                    return vdto;
                })
                .toList();

        dto.setVariants(variants);
        return dto;
    }

}
