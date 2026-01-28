package com.example.SWP391_SPRING2026.DTO;

import com.example.SWP391_SPRING2026.Enum.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private Long id;
    private String accessKey;
    private String refreshKey;
    private UserRole role;
}
