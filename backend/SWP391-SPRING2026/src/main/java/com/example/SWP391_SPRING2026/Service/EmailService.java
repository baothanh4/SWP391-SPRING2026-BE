package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.Repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final OtpVerificationRepository otpVerificationRepository;

    public void sendResetPasswordEmail(String to,String otp){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Reset Password Verification Code");
        message.setText(
                "Your password reset code is:\n\n" +
                        otp + "\n\n" +
                        "This code will expire in 15 minutes.\n" +
                        "Do NOT share this code with anyone."
        );
        mailSender.send(message);
    }

    public void sendRegisterOtpEmail(String to, String otp){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Account Registration Code");
        message.setText(
                "Welcome!\n\n" +
                        "Your account verification code is:\n\n" +
                        otp + "\n\n" +
                        "This code will expire in 15 minutes."
        );
        mailSender.send(message);
    }



}
