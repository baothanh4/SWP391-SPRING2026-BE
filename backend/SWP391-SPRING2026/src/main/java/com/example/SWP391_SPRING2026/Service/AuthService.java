package com.example.SWP391_SPRING2026.Service;


import com.example.SWP391_SPRING2026.DTO.LoginRequest;
import com.example.SWP391_SPRING2026.DTO.LoginResponse;
import com.example.SWP391_SPRING2026.Entity.Users;
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
        Users user=userRepository.findByEmailOrPhone(request.getUsername(),request.getPassword()).orElseThrow(() -> new RuntimeException("User not found"));

        if(!passwordEncoder.matches(request.getPassword(),user.getPassword())){
            throw new RuntimeException("Incorrect email or password");
        }

        return new LoginResponse(
                user.getId(),
                jwtService.generateAccessToken(user),
                jwtService.generateRefreshToken(user),
                user.getRole()
        );
    }
}
