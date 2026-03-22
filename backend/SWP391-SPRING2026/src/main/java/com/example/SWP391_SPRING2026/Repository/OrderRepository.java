package com.example.SWP391_SPRING2026.Repository;

import com.example.SWP391_SPRING2026.Entity.Order;
import com.example.SWP391_SPRING2026.Enum.ApprovalStatus;
import com.example.SWP391_SPRING2026.Enum.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order,Long> {
    Page<Order> findByOrderStatus(OrderStatus status, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> lockById(Long id);
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("""
    SELECT COALESCE(SUM(o.totalAmount),0)
    FROM Order o
    WHERE o.orderStatus = 'COMPLETED'
    """)
    BigDecimal getTotalRevenue();

    @Query("""
    SELECT COUNT(o)
    FROM Order o
    WHERE o.orderStatus = 'COMPLETED'
    """)
    Long getTotalOrders();

    @Query("""
    SELECT COALESCE(SUM(o.totalAmount) / COUNT(o),0)
    FROM Order o
    WHERE o.orderStatus = 'COMPLETED'
    """)
    BigDecimal getAverageOrderValue();

    @Query("""
    SELECT 
    (COUNT(CASE WHEN o.orderStatus = 'CANCELLED' THEN 1 END) * 100.0) / COUNT(o)
    FROM Order o
    """)
    Double getCancellationRate();

    Page<Order> findByApprovalStatusAndOrderStatusNotIn(
            ApprovalStatus approvalStatus,
            List<OrderStatus> excludedStatuses,
            Pageable pageable
    );

    @Query("""
    SELECT o
    FROM Order o
    WHERE o.orderStatus NOT IN :excludedStatuses
      AND (
            o.approvalStatus = :approvalStatus
            OR o.supportApprovedAt IS NOT NULL
          )
    """)
    Page<Order> findOperationApprovedOrders(
            ApprovalStatus approvalStatus,
            List<OrderStatus> excludedStatuses,
            Pageable pageable
    );
}
