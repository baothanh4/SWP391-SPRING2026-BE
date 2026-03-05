package com.example.SWP391_SPRING2026.Entity;

import com.example.SWP391_SPRING2026.Enum.ReturnReason;
import com.example.SWP391_SPRING2026.Enum.ReturnRequestStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "return_requests",
        indexes = {
                @Index(name = "idx_return_requests_status", columnList = "status"),
                @Index(name = "idx_return_requests_order_item", columnList = "order_item_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ERD: OrderItem can have ReturnRequest
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id", nullable = false)
    @JsonIgnore
    private OrderItems orderItem;

    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnReason reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnRequestStatus status;

    @Column(name = "note")
    private String note;

    // MVP: lưu dạng chuỗi (CSV/JSON) cho evidence
    @Column(name = "evidence_urls", columnDefinition = "text")
    private String evidenceUrls;

    // Support audit
    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_by_user_id")
    private Long rejectedByUserId;

    @Column(name = "rejected_note")
    private String rejectedNote;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    // Operation audit
    @Column(name = "received_by_user_id")
    private Long receivedByUserId;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ERD: ReturnRequest triggers Return
    @OneToOne(mappedBy = "returnRequest", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private ReturnRecord returnRecord;
}