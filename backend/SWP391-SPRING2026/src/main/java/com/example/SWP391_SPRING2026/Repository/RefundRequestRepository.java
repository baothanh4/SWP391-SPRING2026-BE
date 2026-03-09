package com.example.SWP391_SPRING2026.Repository;

import com.example.SWP391_SPRING2026.Entity.RefundRequest;
import com.example.SWP391_SPRING2026.Enum.RefundRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {
    List<RefundRequest> findByStatus(RefundRequestStatus status);

    Boolean existsByOrderIdAndStatus(Long orderId, RefundRequestStatus status);

    @Query("""
        select coalesce(sum(r.refundAmount), 0)
        from RefundRequest r
        where r.order.id = :orderId
          and r.status in :statuses
    """)
    Long sumRefundAmountByOrderAndStatuses(Long orderId, Collection<RefundRequestStatus> statuses);
}