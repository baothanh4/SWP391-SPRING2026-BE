package com.example.SWP391_SPRING2026.Repository;

import com.example.SWP391_SPRING2026.Entity.RefundRequest;
import com.example.SWP391_SPRING2026.Enum.RefundRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {

    List<RefundRequest> findByStatus(RefundRequestStatus status);

    List<RefundRequest> findByStatusOrderByIdDesc(RefundRequestStatus status);

    Boolean existsByOrder_IdAndStatus(Long orderId, RefundRequestStatus status);

    @Query("""
        select coalesce(sum(r.refundAmount), 0)
        from RefundRequest r
        where r.order.id = :orderId
          and r.status in :statuses
    """)
    Long sumRefundAmountByOrderAndStatuses(
            @Param("orderId") Long orderId,
            @Param("statuses") Collection<RefundRequestStatus> statuses
    );

    @Query("""
        select r
        from RefundRequest r
        where r.order.user.id = :userId
        order by r.id desc
    """)
    List<RefundRequest> findByCustomerUserId(@Param("userId") Long userId);

    @Query("""
        select r
        from RefundRequest r
        where r.order.id = :orderId
        order by r.id desc
    """)
    List<RefundRequest> findByOrderIdOrderByIdDesc(@Param("orderId") Long orderId);
}