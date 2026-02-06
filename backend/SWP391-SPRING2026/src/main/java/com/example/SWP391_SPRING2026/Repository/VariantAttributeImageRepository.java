package com.example.SWP391_SPRING2026.Repository;

import com.example.SWP391_SPRING2026.Entity.VariantAttributeImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VariantAttributeImageRepository extends JpaRepository<VariantAttributeImage, Long> {
}
