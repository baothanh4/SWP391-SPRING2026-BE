package com.example.SWP391_SPRING2026.DTO.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressUpdateDTO {
    private String receiverName;
    private String phone;
    private String addressLine;
    private String ward;
    private String district;
    private String province;
    private Integer districtId;
    private String wardCode;
}
