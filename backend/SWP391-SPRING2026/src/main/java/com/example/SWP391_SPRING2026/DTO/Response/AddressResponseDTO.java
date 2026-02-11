package com.example.SWP391_SPRING2026.DTO.Response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
