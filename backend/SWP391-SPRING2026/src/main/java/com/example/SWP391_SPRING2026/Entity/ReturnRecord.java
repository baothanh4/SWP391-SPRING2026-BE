package com.example.SWP391_SPRING2026.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "return_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1 ReturnRecord cho 1 ReturnRequest
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_request_id", nullable = false, unique = true)
    @JsonIgnore
    private ReturnRequest returnRequest;

    @Column(name = "accepted_quantity", nullable = false)
    private Integer acceptedQuantity;

    @Column(name = "condition_note")
    private String conditionNote;

    @Column(name = "restocked", nullable = false)
    private Boolean restocked;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}