package com.example.SWP391_SPRING2026.DTO.Request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AddressRequestDTO {

    @NotBlank(message = "Receiver name is required")
    @Size(max = 100, message = "Receiver name must be less than 100 characters")
    private String receiverName;

    @NotBlank(message = "Phone is required")
    @Pattern(
            regexp = "^(0[0-9]{9})$",
            message = "Phone must be 10 digits and start with 0"
    )
    private String phone;

    @NotBlank(message = "Address line is required")
    @Size(max = 255, message = "Address line too long")
    private String addressLine;

    @NotBlank(message = "Ward name is required")
    private String ward;

    @NotBlank(message = "District name is required")
    private String district;

    @NotBlank(message = "Province name is required")
    private String province;

    @NotNull(message = "District ID is required")
    @Positive(message = "District ID must be positive")
    private Integer districtId;

    @NotBlank(message = "Ward code is required")
    private String wardCode;
}
