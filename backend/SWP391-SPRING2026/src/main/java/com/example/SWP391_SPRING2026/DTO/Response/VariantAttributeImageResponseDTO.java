package com.example.SWP391_SPRING2026.DTO.Response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantAttributeImageResponseDTO {
    private Long id;
    private String imageUrl;
    private Integer sortOrder;
    private Long attributeId;


}
