package com.example.SWP391_SPRING2026.Repository;

import com.example.SWP391_SPRING2026.Entity.ReturnRequest;
import com.example.SWP391_SPRING2026.Enum.ReturnRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    boolean existsByOrderItem_IdAndStatusIn(Long orderItemId, Collection<ReturnRequestStatus> statuses);

    List<ReturnRequest> findByOrderItem_Order_Address_User_IdOrderByIdDesc(Long userId);

    List<ReturnRequest> findByStatusOrderByIdDesc(ReturnRequestStatus status);

    Optional<ReturnRequest> findByIdAndOrderItem_Order_Address_User_Id(Long id, Long userId);
}