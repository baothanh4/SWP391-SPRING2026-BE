package com.example.SWP391_SPRING2026.Service;


import com.example.SWP391_SPRING2026.DTO.LoginRequest;
import com.example.SWP391_SPRING2026.DTO.LoginResponse;
import com.example.SWP391_SPRING2026.DTO.RegisterRequest;
import com.example.SWP391_SPRING2026.DTO.RegisterResponse;
import com.example.SWP391_SPRING2026.Entity.Users;
import com.example.SWP391_SPRING2026.Enum.UserRole;
import com.example.SWP391_SPRING2026.Enum.UserStatus;
import com.example.SWP391_SPRING2026.Exception.BadRequestException;
import com.example.SWP391_SPRING2026.Exception.DuplicateResourceException;
import com.example.SWP391_SPRING2026.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JWTService jwtService;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request){
        String username = request.getUsername();
        Users user = userRepository.findByEmailOrPhone(username, username)
                .orElseThrow(() -> new BadRequestException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid username or password");
        }

        return new LoginResponse(
                user.getId(),
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                user.getRole()
        );
    }

    public RegisterResponse register(RegisterRequest request) {
        if (request.getConfirmPassword() != null && !request.getConfirmPassword().isBlank()) {
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                throw new BadRequestException("Confirm password does not match");
            }
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("EMAIL_EXISTS", "Email already exists");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("PHONE_EXISTS", "Phone already exists");
        }
        Users user = new Users();
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setFullName(request.getFullName());
        user.setDob(request.getDob());
        user.setGender(request.getGender());


        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // default server-side
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVED);

        Users saved = userRepository.save(user);
        return new RegisterResponse(
                saved.getId(),
                saved.getEmail(),
                saved.getPhone(),
                saved.getFullName(),
                saved.getRole(),
                saved.getStatus(),
                saved.getCreateAt()
        );
    }
}
