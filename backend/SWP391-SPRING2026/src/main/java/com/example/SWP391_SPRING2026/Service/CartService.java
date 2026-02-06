package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Request.AddToCartDTO;
import com.example.SWP391_SPRING2026.DTO.Request.CartSummaryUpdateDTO;
import com.example.SWP391_SPRING2026.DTO.Request.UpdateCartItemDTO;
import com.example.SWP391_SPRING2026.DTO.Response.CartItemResponseDTO;
import com.example.SWP391_SPRING2026.DTO.Response.CartResponseDTO;
import com.example.SWP391_SPRING2026.Entity.Cart;
import com.example.SWP391_SPRING2026.Entity.CartItem;
import com.example.SWP391_SPRING2026.Entity.Product;
import com.example.SWP391_SPRING2026.Entity.ProductVariant;
import com.example.SWP391_SPRING2026.Entity.Users;
import com.example.SWP391_SPRING2026.Enum.CartStatus;
import com.example.SWP391_SPRING2026.Enum.ProductStatus;
import com.example.SWP391_SPRING2026.Exception.BadRequestException;
import com.example.SWP391_SPRING2026.Exception.InsufficientStockException;
import com.example.SWP391_SPRING2026.Exception.ResourceNotFoundException;
import com.example.SWP391_SPRING2026.Repository.CartItemRepository;
import com.example.SWP391_SPRING2026.Repository.CartRepository;
import com.example.SWP391_SPRING2026.Repository.ProductVariantRepository;
import com.example.SWP391_SPRING2026.Repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository productVariantRepository;

    public CartResponseDTO getCurrentCart(Long userId) {
        Cart cart = getOrCreateActiveCart(userId);
        // fetch join để hạn chế N+1
        Cart full = cartRepository.findCartWithItems(userId, CartStatus.ACTIVE).orElse(cart);
        return mapToResponse(full);
    }

    public CartResponseDTO addToCart(Long userId, AddToCartDTO dto) {
        if(dto.getQuantity() == null || dto.getQuantity() <1){
            throw new RuntimeException("Quantity is empty");
        }
        Cart cart = getOrCreateActiveCart(userId);
        ProductVariant variant = resolveVariant(dto);

        validateSellableAndStock(variant, dto.getQuantity());

        CartItem item = cartItemRepository
                .findByCartIdAndProductVariantId(cart.getId(), variant.getId())
                .orElse(null);

        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setProductVariant(variant);
            item.setQuantity(dto.getQuantity());

            // 🔥 QUAN TRỌNG: add vào cart.items
            cart.getItems().add(item);

        } else {
            int newQty = item.getQuantity() + dto.getQuantity();
            validateSellableAndStock(variant, newQty);
            item.setQuantity(newQty);
        }

        cartItemRepository.save(item);

        return getCurrentCart(userId);

    }

    public CartResponseDTO updateItemQuantity(Long userId, Long itemId, UpdateCartItemDTO dto) {
        if (dto.getQuantity() == null || dto.getQuantity() < 1) {
            throw new BadRequestException("Quantity must be >= 1");
        }

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        // ownership check
        Long ownerId = item.getCart().getUser().getId();
        if (!ownerId.equals(userId)) {
            throw new ResourceNotFoundException("Cart item not found");
        }

        ProductVariant variant = item.getProductVariant();
        validateSellableAndStock(variant, dto.getQuantity());

        item.setQuantity(dto.getQuantity());
        cartItemRepository.save(item);

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
        return getCurrentCart(userId);
    }

    public CartResponseDTO clearCart(Long userId) {
        Cart cart = getOrCreateActiveCart(userId);
        cartItemRepository.deleteByCartId(cart.getId());
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
        if (dto.getProductVariantId() != null) {
            return productVariantRepository.findById(dto.getProductVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product variant not found"));
        }

        if (dto.getProductId() != null) {
            return productVariantRepository.findFirstByProductIdOrderByIdAsc(dto.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("No variant found for product"));
        }

        throw new BadRequestException("productVariantId or productId is required");
    }

    private void validateSellableAndStock(ProductVariant variant, int requestedQty) {
        Product product = variant.getProduct();
        if (product != null && product.getStatus() != null && product.getStatus() != ProductStatus.ACTIVE) {
            throw new ResourceNotFoundException("Product not available");
        }

        int stock = variant.getStockQuantity() == null ? 0 : variant.getStockQuantity();
        if (requestedQty > stock) {
            throw new InsufficientStockException(
                    "Insufficient stock. Requested=" + requestedQty + ", Available=" + stock
            );
        }
    }

    private CartResponseDTO mapToResponse(Cart cart) {
        List<CartItemResponseDTO> itemDTOs = new ArrayList<>();

        BigDecimal subTotal = BigDecimal.ZERO;
        int totalItems = 0;

        for (CartItem item : cart.getItems()) {
            ProductVariant v = item.getProductVariant();
            Product p = v.getProduct();

            BigDecimal unitPrice = v.getPrice() == null ? BigDecimal.ZERO : v.getPrice();
            int qty = item.getQuantity() == null ? 0 : item.getQuantity();
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(qty));

            CartItemResponseDTO dto = new CartItemResponseDTO();
            dto.setProductId(p != null ? p.getId() : null);
            dto.setProductName(p != null ? p.getName() : null);
            dto.setProductImage(p != null ? p.getProductImage() : null);
            dto.setUnitPrice(unitPrice);
            dto.setQuantity(qty);
            dto.setTotalPrice(totalPrice);

            itemDTOs.add(dto);

            subTotal = subTotal.add(totalPrice);
            totalItems += qty;
        }

        BigDecimal discount = calculateDiscount(subTotal, cart.getCouponCode());
        if (discount.compareTo(subTotal) > 0) discount = subTotal;

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
