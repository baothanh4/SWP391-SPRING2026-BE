package com.example.SWP391_SPRING2026.DTO.Request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangePasswordDTO {
    @NotBlank
    private String oldPassword;

    @NotBlank
    @Size(min = 8, max = 15)
    private String newPassword;
}
