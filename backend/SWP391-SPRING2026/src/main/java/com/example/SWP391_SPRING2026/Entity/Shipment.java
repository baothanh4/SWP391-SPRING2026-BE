package com.example.SWP391_SPRING2026.Entity;


import ch.qos.logback.core.status.Status;
import com.example.SWP391_SPRING2026.Enum.ShipmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Entity
@Table(name = "shipments")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentStatus status;

    @Column(name = "ghn_order_code",unique = true)
    private String ghnOrderCode;

    private Long codAmount;

    private Boolean codCollected = false;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @PrePersist
    public void generateCode(){
        if(this.ghnOrderCode == null){
            this.ghnOrderCode = generateRandomCode(14);
        }
    }

    private String generateRandomCode(int length){
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();

        for(int i = 0; i < length; i++){
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }
}
