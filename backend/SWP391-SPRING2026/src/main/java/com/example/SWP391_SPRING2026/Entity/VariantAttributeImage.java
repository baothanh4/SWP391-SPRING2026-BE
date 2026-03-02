package com.example.SWP391_SPRING2026.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "variant_attribute_images")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VariantAttributeImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String imageUrl;

    private Integer sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_attribute_id")
    @JsonIgnore
    private VariantAttribute variantAttribute;
}
