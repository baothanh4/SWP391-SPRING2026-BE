package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Request.AdminUserRequest;
import com.example.SWP391_SPRING2026.DTO.Response.AdminUserResponse;
import com.example.SWP391_SPRING2026.Entity.Users;
import com.example.SWP391_SPRING2026.Enum.UserStatus;
import com.example.SWP391_SPRING2026.Exception.BadRequestException;
import com.example.SWP391_SPRING2026.Exception.DuplicateResourceException;
import com.example.SWP391_SPRING2026.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserResponse createAccount(AdminUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("EMAIL_EXISTS", "Email already exists");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("PHONE_EXISTS", "Phone already exists");
        }

        Users user = new  Users();
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setFullName(request.getFullName());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setGender(request.getGender());
        user.setDob(request.getDob());

        user.setStatus(UserStatus.ACTIVED);
        user.setRole(request.getRole());

        Users savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    public List<AdminUserResponse> getAll(){
        return userRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    public AdminUserResponse getById(Long id){
        Users user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        return mapToResponse(user);
    }

    public AdminUserResponse update(Long id, AdminUserRequest request) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        if (request.getDob() != null) {
            user.setDob(request.getDob());
        }


        return mapToResponse(userRepository.save(user));
    }


    public void disable(Long id) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("User not found"));
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }


    private AdminUserResponse mapToResponse(Users u) {
        return new AdminUserResponse(
                u.getId(),
                u.getEmail(),
                u.getPhone(),
                u.getFullName(),
                u.getRole(),
                u.getStatus(),
                u.getCreateAt()
        );
    }
}
