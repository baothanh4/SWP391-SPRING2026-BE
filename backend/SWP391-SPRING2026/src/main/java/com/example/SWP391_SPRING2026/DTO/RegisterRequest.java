package com.example.SWP391_SPRING2026.DTO;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email is not valid")
    private String email;
    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^(0|\\+84)\\d{9}$",
            message = "Phone number is not valid"
    )

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 15, message = "Password must be between 8 and 15 characters")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 15, message = "Password must be between 8 and 15 characters")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dob;

    // 0=male, 1=female, 2=other
    @NotNull(message = "Gender is required")
    @Min(value = 0, message = "Gender must be between 0 and 2")
    @Max(value = 2, message = "Gender must be between 0 and 2")
    private Integer gender;
}
