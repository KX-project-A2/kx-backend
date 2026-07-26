package com.example.kxbackend.domain.auth.repository;

import com.example.kxbackend.domain.auth.entity.PasswordResetToken;
import com.example.kxbackend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    void deleteByUser(User user);
}
