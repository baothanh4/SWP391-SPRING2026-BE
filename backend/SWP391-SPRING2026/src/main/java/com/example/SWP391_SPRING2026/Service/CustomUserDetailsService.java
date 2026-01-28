package com.example.SWP391_SPRING2026.Service;

import com.example.SWP391_SPRING2026.Entity.Users;
import com.example.SWP391_SPRING2026.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email){
        Users users=userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return User.builder()
                .username(users.getEmail())
                .password(users.getPassword())
                .authorities("ROLE_"+ users.getRole().name())
                .build();
    }
}
