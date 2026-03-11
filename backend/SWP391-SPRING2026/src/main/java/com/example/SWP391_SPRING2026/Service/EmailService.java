package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.Repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
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
    public void sendPreOrderRemainingPaymentEmail(String to,
                                                  String orderCode,
                                                  Long remainingAmount,
                                                  java.time.LocalDate expectedDate) {
        org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your pre-order stock has arrived");
        message.setText(
                "Order: " + orderCode + "\n" +
                        "Expected date: " + expectedDate + "\n" +
                        "Remaining amount: " + remainingAmount + " VND\n\n" +
                        "Please complete the remaining payment so the order can move forward."
        );
        mailSender.send(message);
    }

    public void sendOrderPlacedEmail(String to, String orderCode) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Order placed successfully");

        message.setText(
                "Your order has been placed successfully.\n\n" +
                        "Order code: " + orderCode + "\n\n" +
                        "Thank you for shopping with us."
        );

        mailSender.send(message);
    }

    public void sendPaymentSuccessEmail(String to, String orderCode, Long amount) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Payment successful");

        message.setText(
                "Your payment has been completed successfully.\n\n" +
                        "Order code: " + orderCode + "\n" +
                        "Amount: " + amount + " VND\n\n" +
                        "Thank you for your purchase."
        );

        mailSender.send(message);
    }

    @Async
    public void sendOrderDeliveredEmail(String to, String orderCode) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Order delivered successfully");

        message.setText(
                "Your order has been delivered successfully.\n\n" +
                        "Order code: " + orderCode + "\n\n" +
                        "Thank you for shopping with us."
        );

        mailSender.send(message);
    }


}
