package com.example.SWP391_SPRING2026.Repository;

import com.example.SWP391_SPRING2026.Entity.Shipment;
import com.example.SWP391_SPRING2026.Enum.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByGhnOrderCode(String orderCode);
    List<Shipment> findByStatusNot(ShipmentStatus status);
}
