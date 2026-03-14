package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Request.AddToCartDTO;
import com.example.SWP391_SPRING2026.DTO.Request.CartSummaryUpdateDTO;
import com.example.SWP391_SPRING2026.DTO.Request.CartVariantAttributeDTO;
import com.example.SWP391_SPRING2026.DTO.Request.UpdateCartItemDTO;
import com.example.SWP391_SPRING2026.DTO.Response.CartItemResponseDTO;
import com.example.SWP391_SPRING2026.DTO.Response.CartResponseDTO;
import com.example.SWP391_SPRING2026.DTO.Response.ComboItemDTO;
import com.example.SWP391_SPRING2026.Entity.*;
import com.example.SWP391_SPRING2026.Enum.CartStatus;
import com.example.SWP391_SPRING2026.Enum.ProductStatus;
import com.example.SWP391_SPRING2026.Exception.BadRequestException;
import com.example.SWP391_SPRING2026.Exception.InsufficientStockException;
import com.example.SWP391_SPRING2026.Exception.ResourceNotFoundException;
import com.example.SWP391_SPRING2026.Repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityManager;
import com.example.SWP391_SPRING2026.Enum.VariantAvailabilityStatus;
import com.example.SWP391_SPRING2026.Utility.VariantAvailabilityResolver;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductComboRepository  productComboRepository;
    private final EntityManager entityManager;

    public CartResponseDTO getCurrentCart(Long userId) {
        Cart cart = cartRepository
                .findCartWithItems(userId, CartStatus.ACTIVE)
                .orElseGet(() -> getOrCreateActiveCart(userId));

        return mapToResponse(cart);
    }

    public CartResponseDTO addToCart(Long userId, AddToCartDTO dto) {
        if (dto.getQuantity() == null || dto.getQuantity() < 1) {
            throw new BadRequestException("Quantity must be >= 1");
        }

        Cart cart = getOrCreateActiveCart(userId);

        if (dto.getProductComboId() != null) {

            ProductCombo combo = productComboRepository.findById(dto.getProductComboId())
                    .orElseThrow(() -> new ResourceNotFoundException("Combo not found"));

            CartItem item = cartItemRepository
                    .findByCartIdAndProductComboId(cart.getId(), combo.getId());

            if (item == null) {
                item = new CartItem();
                item.setCart(cart);
                item.setProductCombo(combo);
                item.setQuantity(dto.getQuantity());
                cart.getItems().add(item);
            } else {
                item.setQuantity(item.getQuantity() + dto.getQuantity());
            }

            cartItemRepository.save(item);

            entityManager.flush();
            entityManager.clear();

            return getCurrentCart(userId);
        }

        ProductVariant variant = resolveVariant(dto);

        validateSellableAndStock(variant, dto.getQuantity());

        CartItem item = cartItemRepository
                .findByCartIdAndProductVariantId(cart.getId(), variant.getId())
                .orElse(null);

        if (item == null) {
            // ✅ tạo mới
            if (dto.getQuantity() > 100) {
                throw new BadRequestException("Maximum quantity exceeded");
            }

            item = new CartItem();
            item.setCart(cart);
            item.setProductVariant(variant);
            item.setQuantity(dto.getQuantity());

            cart.getItems().add(item);

        } else {
            // ✅ đã tồn tại → cộng dồn
            int newQty = item.getQuantity() + dto.getQuantity();

            if (newQty > 100) {
                throw new BadRequestException("Maximum quantity exceeded");
            }

            validateSellableAndStock(variant, newQty);
            item.setQuantity(newQty);
        }

        cartItemRepository.save(item);
        entityManager.flush();
        entityManager.clear();
        return getCurrentCart(userId);
    }


    public CartResponseDTO updateItemQuantity(Long userId, Long itemId, UpdateCartItemDTO dto) {
        if (dto.getQuantity() == null || dto.getQuantity() < 1) {
            throw new BadRequestException("Quantity must be >= 1");
        }



        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (item.getProductCombo() != null) {
            item.setQuantity(dto.getQuantity());
            cartItemRepository.save(item);
            return getCurrentCart(userId);
        }

        // ownership check
        Long ownerId = item.getCart().getUser().getId();
        if (!ownerId.equals(userId)) {
            throw new ResourceNotFoundException("Cart item not found");
        }

        ProductVariant variant = item.getProductVariant();
        validateSellableAndStock(variant, dto.getQuantity());

        item.setQuantity(dto.getQuantity());
        cartItemRepository.save(item);

        entityManager.flush();
        entityManager.clear();

        return getCurrentCart(userId);
    }

    public CartResponseDTO removeItem(Long userId, Long itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        Long ownerId = item.getCart().getUser().getId();
        if (!ownerId.equals(userId)) {
            throw new ResourceNotFoundException("Cart item not found");
        }

        cartItemRepository.delete(item);

        entityManager.flush();
        entityManager.clear();
        return getCurrentCart(userId);
    }

    public CartResponseDTO updateSummary(Long userId, CartSummaryUpdateDTO dto) {
        Cart cart = getOrCreateActiveCart(userId);

        if (dto.getCouponCode() != null) {
            String code = dto.getCouponCode().trim();
            cart.setCouponCode(code.isEmpty() ? null : code);
        }

        if (dto.getOrderNote() != null) {
            cart.setOrderNote(dto.getOrderNote());
        }

        if (dto.getRequestInvoice() != null) {
            cart.setRequestInvoice(dto.getRequestInvoice());
        }

        cartRepository.save(cart);

        entityManager.flush();
        entityManager.clear();
        return getCurrentCart(userId);
    }

    public CartResponseDTO clearCart(Long userId) {
        Cart cart = getOrCreateActiveCart(userId);

        cart.getItems().clear();

        entityManager.flush();
        entityManager.clear();
        return getCurrentCart(userId);
    }

    // ----------------- helpers -----------------

    private Cart getOrCreateActiveCart(Long userId) {
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE).orElse(null);
        if (cart != null) return cart;

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Cart newCart = new Cart();
        newCart.setUser(user);
        newCart.setStatus(CartStatus.ACTIVE);
        newCart.setRequestInvoice(false);
        return cartRepository.save(newCart);
    }

    private ProductVariant resolveVariant(AddToCartDTO dto) {

        ProductVariant variant;

        if (dto.getProductVariantId() != null) {
            variant = productVariantRepository.findById(dto.getProductVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product variant not found"));
        }
        else if (dto.getProductId() != null) {
            variant = productVariantRepository
                    .findFirstByProductIdOrderByIdAsc(dto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("No variant found for product"));
        }
        else {
            throw new BadRequestException("productVariantId or productId is required");
        }

        if (variant.getSaleType() == null) {
            throw new BadRequestException("Variant sale type is missing");
        }

        return variant;
    }

    private void validateSellableAndStock(ProductVariant variant, int requestedQty) {

        if (variant == null) {
            throw new ResourceNotFoundException("Variant not found");
        }

        Product product = variant.getProduct();

        if (product != null && product.getStatus() != ProductStatus.ACTIVE) {
            throw new ResourceNotFoundException("Product not available");
        }

        // đảm bảo saleType không null
        if (variant.getSaleType() == null) {
            throw new BadRequestException("Variant sale type is not defined");
        }

        // ================= IN STOCK =================
        if (variant.getSaleType() == com.example.SWP391_SPRING2026.Enum.SaleType.IN_STOCK) {

            int stock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();

            if (stock <= 0) {
                throw new InsufficientStockException("Product is out of stock");
            }

            if (requestedQty > stock) {
                throw new InsufficientStockException(
                        "Insufficient stock. Requested=" + requestedQty + ", Available=" + stock
                );
            }

            return;
        }

        // ================= PRE ORDER =================
        if (variant.getSaleType() == com.example.SWP391_SPRING2026.Enum.SaleType.PRE_ORDER) {

            int limit = variant.getPreorderLimit() == null ? 0 : variant.getPreorderLimit();
            int current = variant.getCurrentPreorders() == null ? 0 : variant.getCurrentPreorders();

            int remainingSlots = Math.max(limit - current, 0);

            if (remainingSlots <= 0) {
                throw new BadRequestException("Pre-order slot is full");
            }

            if (requestedQty > remainingSlots) {
                throw new BadRequestException(
                        "Pre-order quantity exceeds remaining slots. Requested="
                                + requestedQty + ", RemainingSlots=" + remainingSlots
                );
            }

            return;
        }

    }

    private CartResponseDTO mapToResponse(Cart cart) {

        List<CartItemResponseDTO> itemDTOs = new ArrayList<>();
        BigDecimal subTotal = BigDecimal.ZERO;
        int totalItems = 0;

        List<CartItem> safeItems = new ArrayList<>(cart.getItems());

        for (CartItem item : safeItems) {

            // ================= COMBO =================
            if (item.getProductCombo() != null) {

                ProductCombo combo = item.getProductCombo();

                BigDecimal unitPrice = BigDecimal.valueOf(combo.getComboPrice());
                int qty = safeQty(item.getQuantity());
                BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(qty));

                CartItemResponseDTO dto = new CartItemResponseDTO();

                dto.setCartItemId(item.getId());
                dto.setIsCombo(true);

                dto.setComboId(combo.getId());
                dto.setComboName(combo.getName());
                dto.setComboImage(null);

                dto.setUnitPrice(unitPrice);
                dto.setQuantity(qty);
                dto.setTotalPrice(totalPrice);

                dto.setAttributes(List.of());

                // combo items
                List<ComboItemDTO> comboItems = new ArrayList<>();

                if (combo.getItems() != null) {

                    for (ComboItem ci : combo.getItems()) {

                        ComboItemDTO cdto = new ComboItemDTO();

                        cdto.setVariantId(ci.getProductVariant().getId());

                        Product product = ci.getProductVariant().getProduct();
                        cdto.setProductName(product != null ? product.getName() : null);

                        cdto.setQuantity(ci.getQuantity());

                        comboItems.add(cdto);
                    }
                }

                dto.setComboItems(comboItems);

                itemDTOs.add(dto);

                subTotal = subTotal.add(totalPrice);
                totalItems += qty;

                continue;
            }

            // ================= VARIANT =================

            ProductVariant v = item.getProductVariant();
            if (v == null) continue;

            Product p = v.getProduct();

            BigDecimal unitPrice = v.getPrice() == null
                    ? BigDecimal.ZERO
                    : v.getPrice();

            int qty = safeQty(item.getQuantity());
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(qty));

            List<CartVariantAttributeDTO> attrDTOs = new ArrayList<>();

            if (v.getAttributes() != null) {

                List<VariantAttribute> safeAttrs =
                        new ArrayList<>(v.getAttributes());

                for (VariantAttribute attr : safeAttrs) {

                    CartVariantAttributeDTO adto =
                            new CartVariantAttributeDTO();

                    adto.setAttributeName(attr.getAttributeName());
                    adto.setAttributeValue(attr.getAttributeValue());

                    List<String> imageUrls = new ArrayList<>();

                    if (attr.getImages() != null) {

                        List<VariantAttributeImage> safeImages =
                                new ArrayList<>(attr.getImages());

                        safeImages.sort(
                                Comparator.comparing(
                                        VariantAttributeImage::getSortOrder,
                                        Comparator.nullsLast(Integer::compareTo)
                                )
                        );

                        for (VariantAttributeImage img : safeImages) {
                            imageUrls.add(img.getImageUrl());
                        }
                    }

                    adto.setImages(imageUrls);
                    attrDTOs.add(adto);
                }
            }

            String displayImage = attrDTOs.stream()
                    .flatMap(a -> a.getImages().stream())
                    .findFirst()
                    .orElse(p != null ? p.getProductImage() : null);

            CartItemResponseDTO dto = new CartItemResponseDTO();

            dto.setCartItemId(item.getId());
            dto.setIsCombo(false);

            dto.setProductId(p != null ? p.getId() : null);
            dto.setProductName(p != null ? p.getName() : null);
            dto.setProductImage(displayImage);

            dto.setUnitPrice(unitPrice);
            dto.setQuantity(qty);
            dto.setTotalPrice(totalPrice);

            dto.setAttributes(attrDTOs);
            dto.setSaleType(v.getSaleType());
            itemDTOs.add(dto);

            subTotal = subTotal.add(totalPrice);
            totalItems += qty;
        }

        BigDecimal discount = calculateDiscount(subTotal, cart.getCouponCode());

        if (discount.compareTo(subTotal) > 0) {
            discount = subTotal;
        }

        BigDecimal finalTotal = subTotal.subtract(discount);

        CartResponseDTO res = new CartResponseDTO();

        res.setItems(itemDTOs);
        res.setSubTotal(subTotal);
        res.setDiscountAmount(discount);
        res.setFinalTotal(finalTotal);

        res.setCouponCode(cart.getCouponCode());
        res.setOrderNote(cart.getOrderNote());
        res.setRequestInvoice(Boolean.TRUE.equals(cart.getRequestInvoice()));

        res.setTotalItems(totalItems);
        res.setEmpty(itemDTOs.isEmpty());

        return res;
    }

    private int safeQty(Integer qty) {
        return qty == null ? 0 : qty;
    }

    private BigDecimal calculateDiscount(BigDecimal subTotal, String couponCode) {
        if (couponCode == null || couponCode.trim().isEmpty()) return BigDecimal.ZERO;

        String code = couponCode.trim().toUpperCase();

        if ("SALE10".equals(code)) {
            return subTotal.multiply(new BigDecimal("0.10"));
        }

        if ("SALE50K".equals(code)) {
            return new BigDecimal("50000");
        }

        // coupon không hợp lệ -> có thể chọn:
        // 1) throw BadRequestException("Invalid coupon code")
        // 2) discount = 0
        return BigDecimal.ZERO;
    }
}
