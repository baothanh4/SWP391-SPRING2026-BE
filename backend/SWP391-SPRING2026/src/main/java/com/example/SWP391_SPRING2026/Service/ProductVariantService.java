package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Request.ProductVariantRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Response.ProductVariantResponseDTO;
import com.example.SWP391_SPRING2026.Entity.Product;
import com.example.SWP391_SPRING2026.Entity.ProductVariant;
import com.example.SWP391_SPRING2026.Enum.PreOrderStatus;
import com.example.SWP391_SPRING2026.Enum.SaleType;
import com.example.SWP391_SPRING2026.Repository.PreOrderRepository;
import com.example.SWP391_SPRING2026.Repository.ProductRepository;
import com.example.SWP391_SPRING2026.Repository.ProductVariantRepository;
import com.example.SWP391_SPRING2026.Utility.PreOrderRule;
import com.example.SWP391_SPRING2026.Utility.VariantAvailabilityResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductVariantService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final PreOrderRepository preOrderRepository;
    private final PreOrderService preOrderService;

    // Chỉ những line CHƯA được allocate stock mới được chặn chuyển sang IN_STOCK
    private static final EnumSet<PreOrderStatus> PENDING_STOCK_QUEUE_STATUSES =
            EnumSet.of(
                    PreOrderStatus.RESERVED,
                    PreOrderStatus.AWAITING_STOCK
            );

    public ProductVariantResponseDTO create(Long productId, ProductVariantRequestDTO dto) {

        if (productVariantRepository.existsBySku(dto.getSku())) {
            throw new RuntimeException("SKU already exists");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product Not Found"));

        ProductVariant productVariant = new ProductVariant();
        productVariant.setSku(dto.getSku());
        productVariant.setPrice(dto.getPrice());
        productVariant.setStockQuantity(dto.getStockQuantity() == null ? 0 : dto.getStockQuantity());
        productVariant.setSaleType(dto.getSaleType());
        productVariant.setCurrentPreorders(0);
        productVariant.setProduct(product);

        applyPreOrderConfig(productVariant, dto);

        productVariantRepository.save(productVariant);

        return toDTO(productVariant);
    }

    public ProductVariantResponseDTO update(Long variantId, ProductVariantRequestDTO dto) {

        if (productVariantRepository.existsBySkuAndIdNot(dto.getSku(), variantId)) {
            throw new RuntimeException("SKU already exists");
        }

        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Product Variant Not Found"));

        SaleType currentSaleType = variant.getSaleType();
        SaleType newSaleType = dto.getSaleType();

        int oldStock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
        int newStock = dto.getStockQuantity() == null ? 0 : dto.getStockQuantity();

        // Chỉ chặn chuyển PRE_ORDER -> IN_STOCK nếu vẫn còn line chưa được allocate stock
        if (currentSaleType == SaleType.PRE_ORDER && newSaleType == SaleType.IN_STOCK) {
            boolean hasPendingStockQueue = preOrderRepository.existsByProductVariant_IdAndPreorderStatusIn(
                    variantId,
                    PENDING_STOCK_QUEUE_STATUSES
            );

            if (hasPendingStockQueue) {
                throw new RuntimeException(
                        "Cannot switch PRE_ORDER to IN_STOCK while reserved/awaiting-stock preorders still exist"
                );
            }
        }

        variant.setSku(dto.getSku());
        variant.setPrice(dto.getPrice());
        variant.setStockQuantity(newStock);
        variant.setSaleType(newSaleType);

        applyPreOrderConfig(variant, dto);

        productVariantRepository.save(variant);

        // Chỉ allocate sau khi stock mới đã được save vào variant
        if (variant.getSaleType() == SaleType.PRE_ORDER && newStock > oldStock) {
            preOrderService.allocateAvailableStock(variantId);
            preOrderService.convertToInStockIfReady(variantId);
        }

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
                .preorderStartDate(productVariant.getPreorderStartDate())
                .preorderEndDate(productVariant.getPreorderEndDate())
                .preorderFulfillmentDate(productVariant.getPreorderFulfillmentDate())
                .availabilityStatus(VariantAvailabilityResolver.resolve(productVariant))
                .product_id(productVariant.getProduct().getId())
                .build();
    }

    private void applyPreOrderConfig(ProductVariant variant, ProductVariantRequestDTO dto) {

        if (dto.getSaleType() == SaleType.PRE_ORDER) {

            Boolean allowPreorder = Boolean.TRUE.equals(dto.getAllowPreorder());

            if (dto.getPreorderLimit() != null && dto.getPreorderLimit() < 0) {
                throw new RuntimeException("preorderLimit must be >= 0 or null");
            }

            if (dto.getPreorderStartDate() == null) {
                throw new RuntimeException("preorderStartDate is required");
            }

            if (dto.getPreorderEndDate() == null) {
                throw new RuntimeException("preorderEndDate is required");
            }

            if (dto.getPreorderEndDate().isBefore(dto.getPreorderStartDate())) {
                throw new RuntimeException("preorderEndDate must be >= preorderStartDate");
            }

            if (dto.getPreorderFulfillmentDate() == null) {
                throw new RuntimeException("preorderFulfillmentDate is required");
            }

            if (dto.getPreorderFulfillmentDate().isBefore(dto.getPreorderEndDate())) {
                throw new RuntimeException("preorderFulfillmentDate must be on or after preorderEndDate");
            }

            if (dto.getPreorderFulfillmentDate()
                    .isAfter(dto.getPreorderEndDate().plusDays(PreOrderRule.FULFILLMENT_MAX_DAYS_AFTER_END))) {
                throw new RuntimeException("preorderFulfillmentDate cannot be more than 3 days after preorderEndDate");
            }

            int currentPreorders = variant.getCurrentPreorders() == null ? 0 : variant.getCurrentPreorders();
            if (dto.getPreorderLimit() != null && dto.getPreorderLimit() < currentPreorders) {
                throw new RuntimeException("preorderLimit cannot be less than currentPreorders");
            }

            variant.setAllowPreorder(allowPreorder);
            variant.setPreorderLimit(dto.getPreorderLimit());
            variant.setPreorderStartDate(dto.getPreorderStartDate());
            variant.setPreorderEndDate(dto.getPreorderEndDate());
            variant.setPreorderFulfillmentDate(dto.getPreorderFulfillmentDate());
            return;
        }

        variant.setAllowPreorder(false);
        variant.setPreorderLimit(null);
        variant.setPreorderStartDate(null);
        variant.setPreorderEndDate(null);
        variant.setPreorderFulfillmentDate(null);
    }
}