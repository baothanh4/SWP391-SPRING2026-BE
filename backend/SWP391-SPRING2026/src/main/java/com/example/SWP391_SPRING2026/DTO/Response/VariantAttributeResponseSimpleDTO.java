package com.example.SWP391_SPRING2026.DTO.Response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class VariantAttributeResponseSimpleDTO {
    private Long id;
    private String attributeName;
    private String attributeValue;
    private Long product_variant_id;
}
