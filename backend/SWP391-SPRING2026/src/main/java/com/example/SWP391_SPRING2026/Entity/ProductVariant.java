package com.example.SWP391_SPRING2026.Entity;

import com.example.SWP391_SPRING2026.Enum.SaleType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "product_variants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@BatchSize(size = 50)
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sku;

    private BigDecimal price;

    private Integer stockQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "sale_type",nullable = false)
    private SaleType saleType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;

    @OneToMany(mappedBy = "productVariant",cascade = CascadeType.ALL,orphanRemoval = true)
    private Set<VariantAttribute> attributes=new HashSet<>();
}
