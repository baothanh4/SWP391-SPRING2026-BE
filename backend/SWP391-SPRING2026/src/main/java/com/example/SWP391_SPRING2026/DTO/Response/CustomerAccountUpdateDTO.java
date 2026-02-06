package com.example.SWP391_SPRING2026.DTO.Response;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerAccountUpdateDTO {
    @Size(min = 2, message = "Full name is too short")
    private String fullName;

    @Pattern(
            regexp = "^(0|\\+84)\\d{9}$",
            message = "Phone number is not valid"
    )
    private String phone;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dob;

    @Min(0)
    @Max(2)
    private Integer gender;
}
