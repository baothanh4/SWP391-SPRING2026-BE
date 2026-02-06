package com.example.SWP391_SPRING2026.DTO.Request;

import com.example.SWP391_SPRING2026.Enum.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerAccountResponseDTO {
    private Long id;
    private String email;
    private String phone;
    private String fullName;
    private Integer gender;
    private LocalDate dob;
    private UserStatus status;
    private LocalDateTime createdAt;
}
