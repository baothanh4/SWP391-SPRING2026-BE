package com.example.SWP391_SPRING2026.DTO.Request;

import com.example.SWP391_SPRING2026.Enum.UserRole;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserCreateReq {

    @NotBlank(message = "Email là bắt buộc")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Số điện thoại là bắt buộc")
    @Pattern(regexp = "^(0|\\\\+84)\\\\d{9}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @NotBlank(message = "Mật khẩu là bắt buộc")
    @Size(min = 8, max = 15, message = "Mật khẩu phải từ 8 đến 15 ký tự")
    private String password;

    @NotBlank(message = "Họ tên là bắt buộc")
    private String fullName;

    @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    private LocalDate dob;

    // 0: Nam, 1: Nữ, 2: Khác
    @NotNull(message = "Giới tính là bắt buộc")
    @Min(value = 0, message = "Giới tính phải trong khoảng 0..2")
    @Max(value = 2, message = "Giới tính phải trong khoảng 0..2")
    private Integer gender;

    @NotNull(message = "Role là bắt buộc")
    private UserRole role;
}
