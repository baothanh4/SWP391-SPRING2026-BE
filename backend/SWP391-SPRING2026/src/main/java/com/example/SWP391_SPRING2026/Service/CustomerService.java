package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.DTO.Request.ChangePasswordDTO;
import com.example.SWP391_SPRING2026.DTO.Request.CustomerAccountResponseDTO;
import com.example.SWP391_SPRING2026.DTO.Response.CustomerAccountUpdateDTO;
import com.example.SWP391_SPRING2026.Entity.Users;
import com.example.SWP391_SPRING2026.Enum.UserStatus;
import com.example.SWP391_SPRING2026.Exception.BadRequestException;
import com.example.SWP391_SPRING2026.Exception.DuplicateResourceException;
import com.example.SWP391_SPRING2026.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerAccountResponseDTO getProfile(Long userId){
        Users user =  userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        return map(user);
    }

    public CustomerAccountResponseDTO updateProfile(Long userId, CustomerAccountUpdateDTO dto){
        Users user =  userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        if(dto.getFullName() != null){
            user.setFullName(dto.getFullName());
        }

        if(dto.getPhone() != null && !dto.getPhone().equals(user.getPhone())){
            if(userRepository.existsByPhone(dto.getPhone())){
                throw new DuplicateResourceException("PHONE_EXISTS", "Phone already exists");
            }
            user.setPhone(dto.getPhone());
        }

        if(dto.getDob() != null){
            user.setDob(dto.getDob());
        }

        if(dto.getGender() != null){
            user.setGender(dto.getGender());
        }

        return map(userRepository.save(user));
    }

    public void changePassword(Long userId, ChangePasswordDTO dto){
        Users user =  userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        if(!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())){
            throw new BadRequestException("Old Password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    public void disableAccount(Long userId){
        Users user =  userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }


    private CustomerAccountResponseDTO map(Users u) {
        return new CustomerAccountResponseDTO(
                u.getId(),
                u.getEmail(),
                u.getPhone(),
                u.getFullName(),
                u.getGender(),
                u.getDob(),
                u.getStatus(),
                u.getCreateAt()
        );
    }
}
