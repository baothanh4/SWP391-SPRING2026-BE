package com.example.SWP391_SPRING2026.Entity;

import com.example.SWP391_SPRING2026.Enum.SaleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
    private Product product;

    @OneToMany(mappedBy = "productVariant",cascade = CascadeType.ALL,orphanRemoval = true)
    private List<VariantAttribute> attributes=new ArrayList<>();
}
