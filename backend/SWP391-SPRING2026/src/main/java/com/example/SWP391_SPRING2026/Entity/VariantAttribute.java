package com.example.SWP391_SPRING2026.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "variant_attributes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VariantAttribute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String attributeName;
    private String attributeValue;

    @ManyToOne
    @JoinColumn(name = "product_variant_id")
    @JsonIgnore
    private ProductVariant productVariant;

    @OneToMany(
            mappedBy = "variantAttribute",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<VariantAttributeImage> images = new HashSet<>();
}
