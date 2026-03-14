package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.Repository.OtpVerificationRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final OtpVerificationRepository otpVerificationRepository;

    private String formatMoney(Long amount){
        NumberFormat format =
                NumberFormat.getInstance(new Locale("vi","VN"));
        return format.format(amount) + " VND";
    }

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

    public void sendOrderPlacedEmail(String to, String orderCode){

        sendHtmlMail(
                to,
                "Order placed successfully",
                buildOrderPlacedTemplate(orderCode)
        );
    }

    public void sendPaymentSuccessEmail(String to, String orderCode, Long amount){

        sendHtmlMail(
                to,
                "Payment successful",
                buildPaymentSuccessTemplate(orderCode, amount)
        );
    }

    @Async
    public void sendOrderDeliveredEmail(String to, String orderCode){

        sendHtmlMail(
                to,
                "Order delivered successfully",
                buildDeliveredTemplate(orderCode)
        );
    }

    private void sendHtmlMail(String to, String subject, String html){

        try{

            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);

        }catch(Exception e){
            throw new RuntimeException("Cannot send email");
        }
    }

    private String baseTemplate(String content){

        return """
            <html>
            <body style="font-family:Arial;background:#f5f5f5;padding:20px;">
            
            <div style="max-width:600px;margin:auto;background:white;
                        border-radius:10px;overflow:hidden;
                        box-shadow:0 4px 10px rgba(0,0,0,0.1);">
            
                <div style="background:#111;color:white;
                            padding:20px;text-align:center;
                            font-size:22px;font-weight:bold;">
                    SWP Store
                </div>
            
                <div style="padding:30px;">
                
                    %s
                
                </div>
            
                <div style="background:#fafafa;
                            text-align:center;
                            padding:15px;
                            font-size:12px;
                            color:#777;">
                    © 2026 SWP Store
                </div>
            
            </div>
            
            </body>
            </html>
            """.formatted(content);
    }

    private String buildOrderPlacedTemplate(String orderCode){

        String content = """
            <h2 style="color:#333;">Order placed successfully 🎉</h2>
            
            <p>Thank you for shopping with us.</p>
            
            <div style="background:#f8f8f8;padding:15px;
                        border-radius:6px;margin-top:15px;">
            
                <b>Order Code:</b> %s
            
            </div>
            
            <p style="margin-top:20px;">
                We will process your order soon.
            </p>
            """.formatted(orderCode);

        return baseTemplate(content);
    }

    private String buildPaymentSuccessTemplate(String orderCode, Long amount){

        String content = """
            <h2 style="color:#28a745;">Payment Successful ✅</h2>
            
            <p>Your payment has been completed successfully.</p>
            
            <div style="background:#f8f8f8;padding:15px;
                        border-radius:6px;margin-top:15px;">
            
                <p><b>Order Code:</b> %s</p>
                <p><b>Amount:</b> %s</p>
            
            </div>
            
            <p style="margin-top:20px;">
                Thank you for your purchase!
            </p>
            """.formatted(orderCode, formatMoney(amount));

        return baseTemplate(content);
    }

    private String buildDeliveredTemplate(String orderCode){

        String content = """
            <h2 style="color:#007bff;">Order Delivered 📦</h2>
            
            <p>Your order has been delivered successfully.</p>
            
            <div style="background:#f8f8f8;padding:15px;
                        border-radius:6px;margin-top:15px;">
            
                <b>Order Code:</b> %s
            
            </div>
            
            <p style="margin-top:20px;">
                We hope you enjoy your purchase.
            </p>
            """.formatted(orderCode);

        return baseTemplate(content);
    }


}
