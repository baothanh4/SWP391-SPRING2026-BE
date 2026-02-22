package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Request.UserCreateReq;
import com.example.SWP391_SPRING2026.DTO.Request.UserStatusReq;
import com.example.SWP391_SPRING2026.DTO.Request.UserUpdateReq;
import com.example.SWP391_SPRING2026.DTO.Response.AdminUserResponse;
import com.example.SWP391_SPRING2026.Entity.Users;
import com.example.SWP391_SPRING2026.Enum.UserRole;
import com.example.SWP391_SPRING2026.Enum.UserStatus;
import com.example.SWP391_SPRING2026.Exception.BadRequestException;
import com.example.SWP391_SPRING2026.Exception.DuplicateResourceException;
import com.example.SWP391_SPRING2026.Exception.ResourceNotFoundException;
import com.example.SWP391_SPRING2026.Repository.UserRepository;
import com.example.SWP391_SPRING2026.Repository.Specification.UserSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Transactional
    public AdminUserResponse createUser(UserCreateReq request) {

        String email = normalizeEmail(request.getEmail());
        String phone = normalizePhone(request.getPhone());

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("EMAIL_EXISTS", "Email đã tồn tại");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new DuplicateResourceException("PHONE_EXISTS", "Số điện thoại đã tồn tại");
        }

        if (request.getRole() == null) {
            throw new BadRequestException("Role là bắt buộc");
        }
        if (request.getRole() == UserRole.CUSTOMER) {
            throw new BadRequestException("Không tạo CUSTOMER ở Admin. Customer phải đăng ký qua /api/auth/register");
        }

        Users user = new Users();
        user.setEmail(email);
        user.setPhone(phone);
        user.setFullName(normalizeText(request.getFullName()));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setGender(request.getGender());
        user.setDob(request.getDob());


        user.setStatus(UserStatus.ACTIVED);
        user.setRole(request.getRole());

        return mapToResponse(userRepository.save(user));
    }


    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listUsers(String keyword, UserRole role, UserStatus status, Pageable pageable) {

        Specification<Users> spec = (root, query, cb) -> cb.conjunction();

        if (StringUtils.hasText(keyword)) {
            spec = spec.and(UserSpecifications.keywordContains(keyword));
        }
        if (role != null) {
            spec = spec.and(UserSpecifications.hasRole(role));
        }
        if (status != null) {
            spec = spec.and(UserSpecifications.hasStatus(status));
        }

        return userRepository.findAll(spec, pageable).map(this::mapToResponse);
    }


    @Transactional(readOnly = true)
    public AdminUserResponse getById(Long id) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại trong hệ thống"));
        return mapToResponse(user);
    }


    @Transactional
    public AdminUserResponse updateBasic(Long id, UserUpdateReq request) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại trong hệ thống"));

        if (request.getFullName() != null) {
            String fn = normalizeText(request.getFullName());
            if (fn.isBlank()) throw new BadRequestException("Họ tên không được rỗng");
            user.setFullName(fn);
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


    @Transactional
    public AdminUserResponse updateStatus(Long id, UserStatusReq req, Long currentAdminId) {

        Users user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại trong hệ thống"));


        if (currentAdminId != null && id.equals(currentAdminId) && req.getStatus() == UserStatus.INACTIVE) {
            throw new BadRequestException("Admin không được tự khóa chính mình");
        }

        user.setStatus(req.getStatus());
        return mapToResponse(userRepository.save(user));
    }


    @Transactional
    public void disable(Long id, Long currentAdminId) {

        if (currentAdminId != null && id.equals(currentAdminId)) {
            throw new BadRequestException("Admin không được tự khóa chính mình");
        }

        Users user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại trong hệ thống"));

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

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) return "";
        return email.trim().toLowerCase();
    }

    private String normalizePhone(String phone) {
        if (!StringUtils.hasText(phone)) return "";
        return phone.trim();
    }

    private String normalizeText(String s) {
        return s == null ? "" : s.trim();
    }
}
