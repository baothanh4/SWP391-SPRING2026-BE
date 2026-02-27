package com.example.SWP391_SPRING2026.Repository;

import com.example.SWP391_SPRING2026.Entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartIdAndProductVariantId(Long cartId, Long productVariantId);

    List<CartItem> findByCartId(Long cartId);

    void deleteByCartId(Long cartId);

    CartItem findByCartIdAndProductComboId(Long cartId, Long productComboId);
}
