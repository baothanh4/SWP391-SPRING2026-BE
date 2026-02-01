package com.example.SWP391_SPRING2026.Repository;

import com.example.SWP391_SPRING2026.Entity.VariantAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VariantAttributeRepository extends JpaRepository<VariantAttribute,Long> {
    List<VariantAttribute> findByProductVariantId(Long variantId);
}
