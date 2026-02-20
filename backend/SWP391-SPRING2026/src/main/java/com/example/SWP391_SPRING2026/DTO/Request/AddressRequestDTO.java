package com.example.SWP391_SPRING2026.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressRequestDTO {
    @NotBlank
    private String receiverName;

    @NotBlank
    private String phone;

    @NotBlank
    private String addressLine;

    @NotBlank
    private String ward;

    @NotBlank
    private String district;

    @NotBlank
    private String province;

    @NotBlank
    private Integer districtId;

    @NotBlank
    private String wardCode;

}
