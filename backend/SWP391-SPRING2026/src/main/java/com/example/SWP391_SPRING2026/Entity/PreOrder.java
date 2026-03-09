package com.example.SWP391_SPRING2026.Entity;

import com.example.SWP391_SPRING2026.Enum.PreOrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pre_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1 order có thể có nhiều preorder
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // mỗi preorder line map vào 1 order item
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false, unique = true)
    private OrderItems orderItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "expected_release_date", nullable = false)
    private LocalDate expectedReleaseDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "preorder_status", nullable = false)
    private PreOrderStatus preorderStatus;

    @Column(name = "reserved_at", nullable = false)
    private LocalDateTime reservedAt;

    @Column(name = "slot_released", nullable = false)
    private Boolean slotReleased = false;

    @Column(name = "allocated_stock", nullable = false)
    private Boolean allocatedStock = false;
}