package com.example.SWP391_SPRING2026.Controller;


import com.example.SWP391_SPRING2026.DTO.Request.LoginRequest;
import com.example.SWP391_SPRING2026.DTO.Response.LoginResponse;
import com.example.SWP391_SPRING2026.DTO.Request.RegisterRequest;
import com.example.SWP391_SPRING2026.DTO.Response.RegisterResponse;
import com.example.SWP391_SPRING2026.Service.AuthService;
import com.example.SWP391_SPRING2026.Service.PasswordResetService;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final PasswordResetService  passwordResetService;
    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest){

        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok("OTP send to verify");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email){
        passwordResetService.forgotPassword(email);
        return ResponseEntity.ok("OTP sent to email");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam String email,@RequestParam String otp,@RequestParam String newPassword){
        passwordResetService.resetPassword(email,otp,newPassword);
        return ResponseEntity.ok("Password reset completed");
    }

    @PostMapping("/register/verify-otp")
    public ResponseEntity<?> verifyRegisterOtp(@RequestParam String email,@RequestParam String otp){
        authService.verifyRegisterOtp(email,otp);
        return ResponseEntity.ok("Account verification completed");
    }

    @PostMapping("/register/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestParam String email){
        authService.resendRegisterOtp(email);
        return ResponseEntity.ok("OTP resend completed");
    }
}
