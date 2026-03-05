package com.example.SWP391_SPRING2026.Repository;

import com.example.SWP391_SPRING2026.Entity.Payment;
import com.example.SWP391_SPRING2026.Enum.PaymentStage;
import com.example.SWP391_SPRING2026.Enum.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrder_Id(Long orderId);

    Optional<Payment> findTopByOrder_IdAndStageOrderByIdDesc(Long orderId, PaymentStage stage);

    boolean existsByOrder_IdAndStatus(Long orderId, PaymentStatus status);

    @Query("""
        SELECT p FROM Payment p
        JOIN p.order o
        WHERE o.address.user.id = :userId
        ORDER BY p.createdAt DESC
    """)
    List<Payment> findByUserId(Long userId);
}
