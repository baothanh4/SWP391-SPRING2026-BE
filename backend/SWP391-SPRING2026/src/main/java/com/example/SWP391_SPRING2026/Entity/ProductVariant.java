package com.example.SWP391_SPRING2026.Entity;

import com.example.SWP391_SPRING2026.Enum.SaleType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
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

    @Column(name = "allow_preorder", nullable = false)
    private Boolean allowPreorder = false;

    @Column(name = "preorder_limit")
    private Integer preorderLimit;

    @Column(name = "current_preorders", nullable = false)
    private Integer currentPreorders = 0;

    @Column(name = "preorder_fulfillment_date")
    private LocalDate preorderFulfillmentDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;

    @OneToMany(mappedBy = "productVariant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VariantAttribute> attributes = new ArrayList<>();

    @Column(name = "preorder_start_date")
    private LocalDate preorderStartDate;

    @Column(name = "preorder_end_date")
    private LocalDate preorderEndDate;
}
