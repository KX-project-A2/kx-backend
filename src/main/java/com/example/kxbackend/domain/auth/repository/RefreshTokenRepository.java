package com.example.kxbackend.domain.auth.repository;

import com.example.kxbackend.domain.auth.entity.RefreshToken;
import com.example.kxbackend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByToken(String token);

    void deleteByUser(User user);
}
