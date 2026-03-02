package com.example.SWP391_SPRING2026.Repository;

import com.example.SWP391_SPRING2026.Entity.Cart;
import com.example.SWP391_SPRING2026.Enum.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @Query("""
    SELECT DISTINCT c FROM Cart c
    LEFT JOIN FETCH c.items i
    LEFT JOIN FETCH i.productCombo
    LEFT JOIN FETCH i.productVariant v
    LEFT JOIN FETCH v.product
    WHERE c.user.id = :userId
      AND c.status = :status
""")
    Optional<Cart> findCartWithItems(@Param("userId") Long userId,
                                     @Param("status") CartStatus status);

    Optional<Cart> findByUserIdAndStatus(Long userId, CartStatus status);
}
