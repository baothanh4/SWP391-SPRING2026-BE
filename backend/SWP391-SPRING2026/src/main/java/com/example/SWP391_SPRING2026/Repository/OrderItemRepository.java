package com.example.SWP391_SPRING2026.Repository;

import com.example.SWP391_SPRING2026.Entity.OrderItems;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItems, Long> {

    // lock + check item thuộc user
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select oi from OrderItems oi
        join oi.order o
        join o.address a
        join a.user u
        where oi.id = :orderItemId and u.id = :userId
    """)
    Optional<OrderItems> lockOwnedItem(Long orderItemId, Long userId);

    // dùng cho support/operation lock theo id
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select oi from OrderItems oi where oi.id = :id")
    java.util.Optional<OrderItems> lockById(Long id);
}