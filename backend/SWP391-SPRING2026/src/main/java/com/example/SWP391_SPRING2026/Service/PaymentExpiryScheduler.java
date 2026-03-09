package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.Entity.Order;
import com.example.SWP391_SPRING2026.Entity.Payment;
import com.example.SWP391_SPRING2026.Enum.OrderStatus;
import com.example.SWP391_SPRING2026.Enum.OrderType;
import com.example.SWP391_SPRING2026.Enum.PaymentStage;
import com.example.SWP391_SPRING2026.Enum.PaymentStatus;
import com.example.SWP391_SPRING2026.Enum.ShipmentStatus;
import com.example.SWP391_SPRING2026.Repository.PaymentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentExpiryScheduler {

    private final PaymentRepository paymentRepository;
    private final PreOrderService preOrderService;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void expirePendingVnPayPayments() {
        List<Payment> expired = paymentRepository.findByStatusAndExpiresAtBefore(
                PaymentStatus.PENDING,
                LocalDateTime.now()
        );

        for (Payment payment : expired) {
            payment.setStatus(PaymentStatus.FAILED);

            Order order = payment.getOrder();

            if (order.getOrderType() == OrderType.PRE_ORDER
                    && (payment.getStage() == PaymentStage.DEPOSIT || payment.getStage() == PaymentStage.FULL)
                    && !paymentRepository.existsByOrder_IdAndStatus(order.getId(), PaymentStatus.SUCCESS)) {

                order.setOrderStatus(OrderStatus.FAILED);

                if (order.getShipment() != null) {
                    order.getShipment().setStatus(ShipmentStatus.CANCELLED);
                }

                preOrderService.releaseReservations(order);
            }
        }
    }
}