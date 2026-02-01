package com.example.SWP391_SPRING2026.DTO.Response;

import lombok.Data;

@Data
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
