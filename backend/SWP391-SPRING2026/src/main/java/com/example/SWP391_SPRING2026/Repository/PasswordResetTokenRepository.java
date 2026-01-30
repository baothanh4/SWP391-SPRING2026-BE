package com.example.SWP391_SPRING2026.Repository;

import com.example.SWP391_SPRING2026.Entity.PasswordResetToken;
import com.example.SWP391_SPRING2026.Entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken,Long> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByUserAndUsedFalse(Users user);
    Optional<PasswordResetToken> findByUser(Users user);
}
