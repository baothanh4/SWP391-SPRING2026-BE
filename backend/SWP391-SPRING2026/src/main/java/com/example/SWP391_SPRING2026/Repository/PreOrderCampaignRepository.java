package com.example.SWP391_SPRING2026.Repository;

import com.example.SWP391_SPRING2026.Entity.PreOrderCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PreOrderCampaignRepository extends JpaRepository<PreOrderCampaign, Long> {

    @Query("""
            select distinct c
            from PreOrderCampaign c
            join c.campaignVariants cv
            join cv.variant v
            where c.isActive = true
              and v.id in :variantIds
              and c.startDate <= :endDate
              and c.endDate >= :startDate
            """)
    List<PreOrderCampaign> findActiveOverlappingCampaigns(
            @Param("variantIds") List<Long> variantIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            select distinct c
            from PreOrderCampaign c
            join c.campaignVariants cv
            join cv.variant v
            where c.id <> :campaignId
              and c.isActive = true
              and v.id in :variantIds
              and c.startDate <= :endDate
              and c.endDate >= :startDate
            """)
    List<PreOrderCampaign> findActiveOverlappingCampaignsExcludingId(
            @Param("campaignId") Long campaignId,
            @Param("variantIds") List<Long> variantIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("""
            select c
            from PreOrderCampaign c
            join c.campaignVariants cv
            join cv.variant v
            where v.id = :variantId
              and c.isActive = true
              and c.startDate <= :today
              and c.endDate >= :today
            order by c.startDate desc, c.id desc
            """)
    List<PreOrderCampaign> findActiveCampaignsForVariant(
            @Param("variantId") Long variantId,
            @Param("today") LocalDate today
    );
}
