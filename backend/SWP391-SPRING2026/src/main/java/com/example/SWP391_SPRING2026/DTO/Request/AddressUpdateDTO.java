package com.example.SWP391_SPRING2026.DTO.Request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AddressUpdateDTO {

    @Size(max = 100, message = "Receiver name must be less than 100 characters")
    private String receiverName;

    @Pattern(
            regexp = "^(0[0-9]{9})$",
            message = "Phone must be 10 digits and start with 0"
    )
    private String phone;

    @Size(max = 255, message = "Address line too long")
    private String addressLine;

    private String ward;

    private String district;

    private String province;

    @Positive(message = "District ID must be positive")
    private Integer districtId;

    private String wardCode;
}
