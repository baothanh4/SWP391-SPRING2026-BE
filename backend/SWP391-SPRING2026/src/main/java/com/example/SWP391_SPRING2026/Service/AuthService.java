package com.example.SWP391_SPRING2026.Service;


import com.example.SWP391_SPRING2026.DTO.Request.LoginRequest;
import com.example.SWP391_SPRING2026.DTO.Response.LoginResponse;
import com.example.SWP391_SPRING2026.DTO.Request.RegisterRequest;
import com.example.SWP391_SPRING2026.DTO.Response.RegisterResponse;
import com.example.SWP391_SPRING2026.Entity.OtpVerification;
import com.example.SWP391_SPRING2026.Entity.Users;
import com.example.SWP391_SPRING2026.Enum.UserRole;
import com.example.SWP391_SPRING2026.Enum.UserStatus;
import com.example.SWP391_SPRING2026.Exception.BadRequestException;
import com.example.SWP391_SPRING2026.Exception.DuplicateResourceException;
import com.example.SWP391_SPRING2026.Repository.OtpVerificationRepository;
import com.example.SWP391_SPRING2026.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JWTService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final OtpVerificationRepository  otpVerificationRepository;
    private final EmailService emailService;

    public LoginResponse login(LoginRequest request){
        String username = request.getUsername();
        Users user = userRepository.findByEmailOrPhone(username, username)
                .orElseThrow(() -> new BadRequestException("Invalid username or password"));

        if(user.getStatus()== UserStatus.INACTIVE){
            throw new BadRequestException("Account inactive.");
        }
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
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadRequestException("Password is required");
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

        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.INACTIVE);

        Users users = userRepository.save(user);

        String otp = String.valueOf((int) (Math.random() * 900000+100000));

        otpVerificationRepository.deleteByEmail(users.getEmail());

        OtpVerification otpVerification= new OtpVerification();
        otpVerification.setEmail(user.getEmail());
        otpVerification.setOtp(otp);
        otpVerification.setExpiredAt(LocalDateTime.now().plusMinutes(15));
        otpVerification.setVerified(false);

        otpVerificationRepository.save(otpVerification);

        emailService.sendRegisterOtpEmail(user.getEmail(), otp);

        return new RegisterResponse(
                users.getId(),
                users.getEmail(),
                users.getPhone(),
                users.getFullName(),
                users.getRole(),
                users.getStatus(),
                users.getCreateAt()
        );
    }

    public void verifyRegisterOtp(String email, String otp){
        OtpVerification otpVerification=otpVerificationRepository.findByEmailAndOtpAndVerifiedFalse(email,otp).orElseThrow(() -> new RuntimeException("Invalid OTP"));

        if(otpVerification.getExpiredAt().isBefore(LocalDateTime.now())){
            throw new RuntimeException("OTP is expired");
        }

        otpVerification.setVerified(true);
        otpVerificationRepository.save(otpVerification);

        Users user=userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Email not found"));
        user.setStatus(UserStatus.ACTIVED);
        userRepository.save(user);
    }

    public void resendRegisterOtp(String email){
        Users user=userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Email not found"));

        if(user.getStatus()== UserStatus.ACTIVED){
            throw new BadRequestException("Account is already activated");
        }

        String otp = String.valueOf((int) (Math.random() * 900000+100000));

        otpVerificationRepository.deleteByEmail(user.getEmail());

        OtpVerification otpVerification=new OtpVerification();
        otpVerification.setEmail(email);
        otpVerification.setOtp(otp);
        otpVerification.setExpiredAt(LocalDateTime.now().plusMinutes(15));
        otpVerification.setVerified(false);

        otpVerificationRepository.save(otpVerification);
        emailService.sendRegisterOtpEmail(user.getEmail(), otp);
    }
}
