package com.example.SWP391_SPRING2026.DTO.Response;

import com.example.SWP391_SPRING2026.Enum.UserRole;
import com.example.SWP391_SPRING2026.Enum.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String email;
    private String phone;
    private String fullName;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;
}
