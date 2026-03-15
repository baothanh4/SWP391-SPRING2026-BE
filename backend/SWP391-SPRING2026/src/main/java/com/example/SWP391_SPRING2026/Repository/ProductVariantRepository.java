package com.example.SWP391_SPRING2026.Repository;

import com.example.SWP391_SPRING2026.DTO.Response.LowStockDTO;
import com.example.SWP391_SPRING2026.Entity.ProductVariant;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant,Long> {
    List<ProductVariant> findByProductId(Long productId);
    Optional<ProductVariant> findFirstByProductIdOrderByIdAsc(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pv from ProductVariant pv where pv.id = :id")
    Optional<ProductVariant> lockById(Long id);

    @Query("""
    SELECT new com.example.SWP391_SPRING2026.DTO.Response.LowStockDTO(
        pv.id,
        p.name,
        pv.stockQuantity
    )
    FROM ProductVariant pv
    JOIN pv.product p
    WHERE pv.stockQuantity < :threshold
    ORDER BY pv.stockQuantity ASC
    """)
    List<LowStockDTO> findLowStockProducts(Integer threshold);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);
}
