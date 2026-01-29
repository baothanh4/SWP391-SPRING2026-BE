package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.Entity.PasswordResetToken;
import com.example.SWP391_SPRING2026.Entity.Users;
import com.example.SWP391_SPRING2026.Repository.PasswordResetTokenRepository;
import com.example.SWP391_SPRING2026.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public void forgotPassword(String email){
        Users user=userRepository.findByEmail(email).orElseThrow(()->new RuntimeException("Email not found"));
        String otp=generateOtp();

        PasswordResetToken token=new PasswordResetToken();
        token.setToken(otp);
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        token.setUsed(false);

        passwordResetTokenRepository.save(token);
        emailService.sendResetPasswordEmail(email,otp);
    }

    public void resetPassword(String email,String otp,String newPassword){
        Users user=userRepository.findByEmail(email).orElseThrow(()->new RuntimeException("Email not found"));

        PasswordResetToken token=passwordResetTokenRepository.findByToken(otp).orElseThrow(()->new RuntimeException("Invalid OTP"));

        if(token.isUsed()){
            throw new RuntimeException("OTP Already Used");
        }

        if(token.getExpiryDate().isBefore(LocalDateTime.now())){
            throw new RuntimeException("OTP Expired");
        }

        if(!token.getUser().getId().equals(user.getId())){
            throw new RuntimeException("User does not belong to this User");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);
    }

    private String generateOtp(){
        return String.valueOf(100000+new SecureRandom().nextInt(900000));
    }
}
