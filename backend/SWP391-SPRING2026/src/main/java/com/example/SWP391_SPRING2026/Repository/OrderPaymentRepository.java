package com.example.SWP391_SPRING2026.Repository;

import com.example.SWP391_SPRING2026.Entity.OrderPayment;
import com.example.SWP391_SPRING2026.Enum.PaymentStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderPaymentRepository extends JpaRepository<OrderPayment, Long> {

    List<OrderPayment> findByOrder_Id(Long orderId);

    Optional<OrderPayment> findTopByOrder_IdAndStageOrderByIdDesc(Long orderId, PaymentStage stage);
}