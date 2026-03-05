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
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.Period;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JWTService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final OtpVerificationRepository  otpVerificationRepository;
    private final EmailService emailService;
    private static final Pattern PHONE_VN = Pattern.compile("^(0|\\+84)(3|5|7|8|9)\\d{8}$");

    // Email chặt hơn @Email: bắt buộc domain có dấu chấm, hạn chế ký tự bậy
    private static final Pattern EMAIL_STRICT = Pattern.compile(
            "^[A-Za-z0-9](?:[A-Za-z0-9._%+-]{0,62}[A-Za-z0-9])?@" +
                    "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?" +
                    "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$"
    );

    private void validateRegisterInput(RegisterRequest request) {
        // email
        String email = request.getEmail();
        if (email == null || email.isBlank()) throw new BadRequestException("Email is required");
        email = email.trim().toLowerCase();
        if (email.length() > 254 || email.contains("..") || !EMAIL_STRICT.matcher(email).matches()) {
            throw new BadRequestException("Email is not valid");
        }
        request.setEmail(email);

        // phone
        String phone = request.getPhone();
        if (phone == null || phone.isBlank()) throw new BadRequestException("Phone number is required");
        phone = phone.trim();
        if (!PHONE_VN.matcher(phone).matches()) throw new BadRequestException("Phone number is not valid");
        request.setPhone(phone);

        // full name
        if (request.getFullName() != null) request.setFullName(request.getFullName().trim());

        // password
        if (request.getPassword() == null || request.getPassword().isBlank())
            throw new BadRequestException("Password is required");
        if (request.getPassword().length() < 8 || request.getPassword().length() > 15)
            throw new BadRequestException("Password must be between 8 and 15 characters");

        if (request.getConfirmPassword() == null || request.getConfirmPassword().isBlank())
            throw new BadRequestException("Confirm password is required");
        if (!request.getPassword().equals(request.getConfirmPassword()))
            throw new BadRequestException("Confirm password does not match");

        // dob + age >= 16
        LocalDate dob = request.getDob();
        if (dob == null) throw new BadRequestException("Date of birth is required");
        if (!dob.isBefore(LocalDate.now())) throw new BadRequestException("Date of birth must be in the past");
        int age = Period.between(dob, LocalDate.now()).getYears();
        if (age < 16) throw new BadRequestException("User must be at least 16 years old");
    }
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
                user.getRole(),
                user.getFullName()
        );
    }
    @Transactional
    public void register(RegisterRequest request) {
        validateRegisterInput(request);

        // Dọn rác bug cũ: nếu có user INACTIVE chiếm email/phone thì xóa để đăng ký lại không bị duplicate
        Users byEmail = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (byEmail != null) {
            if (byEmail.getStatus() == UserStatus.ACTIVE)
                throw new DuplicateResourceException("EMAIL_EXISTS", "Email already exists");
            userRepository.delete(byEmail);
        }

        Users byPhone = userRepository.findByPhone(request.getPhone()).orElse(null);
        if (byPhone != null) {
            if (byPhone.getStatus() == UserStatus.ACTIVE)
                throw new DuplicateResourceException("PHONE_EXISTS", "Phone already exists");
            // tránh delete 2 lần nếu trùng record
            if (byEmail == null || !byPhone.getId().equals(byEmail.getId())) {
                userRepository.delete(byPhone);
            }
        }

        String otp = String.valueOf(100000 + new SecureRandom().nextInt(900000));

        // mỗi email chỉ 1 pending
        otpVerificationRepository.deleteByEmail(request.getEmail());

        OtpVerification pending = new OtpVerification();
        pending.setEmail(request.getEmail());
        pending.setOtp(otp);
        pending.setExpiredAt(LocalDateTime.now().plusMinutes(15));
        pending.setVerified(false);

        // lưu payload đăng ký (đã encode password)
        pending.setPhone(request.getPhone());
        pending.setFullName(request.getFullName());
        pending.setDob(request.getDob());
        pending.setGender(request.getGender());
        pending.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        otpVerificationRepository.save(pending);

        try {
            emailService.sendRegisterOtpEmail(request.getEmail(), otp);
        } catch (Exception e) {

            throw new BadRequestException("Cannot send OTP right now. Please try again later.");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void verifyRegisterOtp(String email, String otp) {
        email = email.trim().toLowerCase();

        OtpVerification pending = otpVerificationRepository
                .findByEmailAndOtpAndVerifiedFalse(email, otp)
                .orElseThrow(() -> new BadRequestException("Invalid OTP"));

        if (pending.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP is expired");
        }

        // chống trùng do race
        Users existedEmail = userRepository.findByEmail(email).orElse(null);
        if (existedEmail != null && existedEmail.getStatus() == UserStatus.ACTIVE) {
            throw new DuplicateResourceException("EMAIL_EXISTS", "Email already exists");
        }
        Users existedPhone = userRepository.findByPhone(pending.getPhone()).orElse(null);
        if (existedPhone != null && existedPhone.getStatus() == UserStatus.ACTIVE) {
            throw new DuplicateResourceException("PHONE_EXISTS", "Phone already exists");
        }

        Users user = new Users();
        user.setEmail(pending.getEmail());
        user.setPhone(pending.getPhone());
        user.setFullName(pending.getFullName());
        user.setDob(pending.getDob());
        user.setGender(pending.getGender());
        user.setPassword(pending.getPasswordHash());
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);

        userRepository.save(user);

        pending.setVerified(true);
        otpVerificationRepository.save(pending);
    }

    public void resendRegisterOtp(String email) {
        email = email.trim().toLowerCase();

        Users user = userRepository.findByEmail(email).orElse(null);
        if (user != null && user.getStatus() == UserStatus.ACTIVE) {
            throw new BadRequestException("Account is already activated");
        }

        OtpVerification pending = otpVerificationRepository
                .findByEmailAndVerifiedFalse(email)
                .orElseThrow(() -> new BadRequestException("No pending registration for this email. Please register again."));

        String otp = String.valueOf(100000 + new SecureRandom().nextInt(900000));
        pending.setOtp(otp);
        pending.setExpiredAt(LocalDateTime.now().plusMinutes(15));
        otpVerificationRepository.save(pending);

        emailService.sendRegisterOtpEmail(email, otp);
    }
}
