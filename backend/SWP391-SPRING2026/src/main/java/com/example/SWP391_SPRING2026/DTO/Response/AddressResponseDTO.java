package com.example.SWP391_SPRING2026.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddressResponseDTO {
    private Long id;
    private String receiverName;
    private String phone;
    private String addressLine;
    private String ward;
    private String district;
    private String province;
    private Boolean isDefault;

}
