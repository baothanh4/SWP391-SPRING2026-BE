package com.example.SWP391_SPRING2026.Repository;

import com.example.SWP391_SPRING2026.Entity.RefundRequest;
import com.example.SWP391_SPRING2026.Enum.RefundRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {
    List<RefundRequest> findByStatus(RefundRequestStatus status);
}