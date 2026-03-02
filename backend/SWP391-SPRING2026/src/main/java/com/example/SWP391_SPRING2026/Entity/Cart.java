package com.example.SWP391_SPRING2026.Entity;

import com.example.SWP391_SPRING2026.Enum.CartStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "carts")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Guest support (optional)
    @Column(name = "session_id")
    private String sessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CartStatus status = CartStatus.ACTIVE;

    @Column(name = "coupon_code")
    private String couponCode;

    @Column(name = "order_note", columnDefinition = "TEXT")
    private String orderNote;

    @Column(name = "request_invoice", nullable = false)
    private Boolean requestInvoice = false;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
