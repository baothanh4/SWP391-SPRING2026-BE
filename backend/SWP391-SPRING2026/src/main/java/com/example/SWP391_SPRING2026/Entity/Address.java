package com.example.SWP391_SPRING2026.Entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "address")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String receiverName;

    private String phone;

    private String addressLine;

    private String ward;

    private String district;

    private String province;

    private Integer districtId;
    private String wardCode;

    @Column(name = "is_default")
    private Boolean isDefault;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id",nullable = false)
    private Users user;

    @OneToMany(mappedBy = "address")
    private List<Order> orders = new ArrayList<>();
}
