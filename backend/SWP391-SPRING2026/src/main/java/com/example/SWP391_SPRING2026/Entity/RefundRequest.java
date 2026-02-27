package com.example.SWP391_SPRING2026.Entity;

import com.example.SWP391_SPRING2026.Enum.RefundPolicy;
import com.example.SWP391_SPRING2026.Enum.RefundReason;
import com.example.SWP391_SPRING2026.Enum.RefundRequestStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "refund_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundReason reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundPolicy policy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundRequestStatus status;

    @Column(name = "refund_amount", nullable = false)
    private Long refundAmount;

    @Column(name = "note")
    private String note;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "created_by_role")
    private String createdByRole;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}