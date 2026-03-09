package com.example.SWP391_SPRING2026.Repository;

import com.example.SWP391_SPRING2026.Entity.PreOrder;
import com.example.SWP391_SPRING2026.Enum.PreOrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface PreOrderRepository extends JpaRepository<PreOrder, Long> {

    List<PreOrder> findByOrder_Id(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select p from PreOrder p
        where p.productVariant.id = :variantId
          and p.preorderStatus in :statuses
        order by p.reservedAt asc, p.id asc
    """)
    List<PreOrder> lockQueueByVariant(Long variantId, Collection<PreOrderStatus> statuses);

    @Query("""
        select p from PreOrder p
        where p.order.id = :orderId
          and p.preorderStatus in :statuses
    """)
    List<PreOrder> findByOrderIdAndStatuses(Long orderId, Collection<PreOrderStatus> statuses);
}