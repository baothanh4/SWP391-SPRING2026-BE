package com.example.SWP391_SPRING2026.Controller;

import com.example.SWP391_SPRING2026.DTO.Request.UserCreateReq;
import com.example.SWP391_SPRING2026.DTO.Request.UserStatusReq;
import com.example.SWP391_SPRING2026.DTO.Request.UserUpdateReq;
import com.example.SWP391_SPRING2026.DTO.Response.AdminUserResponse;
import com.example.SWP391_SPRING2026.Entity.UserPrincipal;
import com.example.SWP391_SPRING2026.Enum.UserRole;
import com.example.SWP391_SPRING2026.Enum.UserStatus;
import com.example.SWP391_SPRING2026.Service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;


    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<AdminUserResponse> createUser(@Valid @RequestBody UserCreateReq request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createUser(request));
    }


    @GetMapping("/users")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Page<AdminUserResponse>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @PageableDefault(size = 10, sort = "createAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(adminService.listUsers(keyword, role, status, pageable));
    }


    @GetMapping("/users/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<AdminUserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.getById(id));
    }


    @PutMapping("/users/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<AdminUserResponse> updateById(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateReq request
    ) {
        return ResponseEntity.ok(adminService.updateBasic(id, request));
    }


    @PatchMapping("/users/{id}/status")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<AdminUserResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusReq req,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentAdminId = (principal != null) ? principal.getUserId() : null;
        return ResponseEntity.ok(adminService.updateStatus(id, req, currentAdminId));
    }


    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<?> disable(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long currentAdminId = (principal != null) ? principal.getUserId() : null;
        adminService.disable(id, currentAdminId);
        return ResponseEntity.ok("User disabled completed");
    }
}
