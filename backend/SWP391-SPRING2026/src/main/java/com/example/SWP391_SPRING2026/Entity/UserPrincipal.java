package com.example.SWP391_SPRING2026.Entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Getter
@AllArgsConstructor
public class UserPrincipal {
    private Long userId;
    private String role;
}
