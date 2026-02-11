package com.example.SWP391_SPRING2026.Controller;

import com.example.SWP391_SPRING2026.DTO.Request.AddToCartDTO;
import com.example.SWP391_SPRING2026.DTO.Request.CartSummaryUpdateDTO;
import com.example.SWP391_SPRING2026.DTO.Request.CheckoutRequestDTO;
import com.example.SWP391_SPRING2026.DTO.Request.UpdateCartItemDTO;
import com.example.SWP391_SPRING2026.DTO.Response.CartResponseDTO;
import com.example.SWP391_SPRING2026.DTO.Response.OrderResponseDTO;
import com.example.SWP391_SPRING2026.Entity.Order;
import com.example.SWP391_SPRING2026.Entity.UserPrincipal;
import com.example.SWP391_SPRING2026.Service.CartService;
import com.example.SWP391_SPRING2026.Service.CheckoutService;
import com.example.SWP391_SPRING2026.mapper.OrderMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CheckoutService checkoutService;

    @GetMapping
    public CartResponseDTO getCart(@AuthenticationPrincipal UserPrincipal principal) {
        return cartService.getCurrentCart(principal.getUserId());
    }

    @PostMapping("/add")
    public CartResponseDTO addToCart(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddToCartDTO dto
    ) {
        return cartService.addToCart(principal.getUserId(), dto);
    }

    @PutMapping("/items/{itemId}")
    public CartResponseDTO updateItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemDTO dto
    ) {
        return cartService.updateItemQuantity(principal.getUserId(), itemId, dto);
    }

    @DeleteMapping("/items/{itemId}")
    public CartResponseDTO removeItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long itemId
    ) {
        return cartService.removeItem(principal.getUserId(), itemId);
    }

    @PutMapping("/summary")
    public CartResponseDTO updateSummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CartSummaryUpdateDTO dto
    ) {
        return cartService.updateSummary(principal.getUserId(), dto);
    }

    @DeleteMapping("/clear")
    public CartResponseDTO clearCart(@AuthenticationPrincipal UserPrincipal principal) {
        return cartService.clearCart(principal.getUserId());
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponseDTO> checkout(@AuthenticationPrincipal UserPrincipal principal, @RequestBody CheckoutRequestDTO dto) {
        Order order = checkoutService.checkout(principal.getUserId(), dto);
        return ResponseEntity.ok(OrderMapper.toResponse(order));
    }
}
