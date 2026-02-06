package com.example.SWP391_SPRING2026.Controller;

import com.example.SWP391_SPRING2026.DTO.Request.AdminUserRequest;
import com.example.SWP391_SPRING2026.DTO.Response.AdminUserResponse;
import com.example.SWP391_SPRING2026.Entity.Users;
import com.example.SWP391_SPRING2026.Service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;

    @PostMapping("/users")
    public ResponseEntity<AdminUserResponse> createUser(@Valid @RequestBody AdminUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createAccount(request));
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> getUsers() {
        return ResponseEntity.ok(adminService.getAll());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserResponse> getById(@PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(adminService.getById(id));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<AdminUserResponse> updateById(@PathVariable(name = "id") Long id, @Valid @RequestBody AdminUserRequest request) {
        return ResponseEntity.ok(adminService.update(id, request));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> disable(@PathVariable(name = "id") Long id) {
        adminService.disable(id);
        return ResponseEntity.ok("User disabled completed");
    }
}
