package com.example.kxbackend.domain.auth.service;

import com.example.kxbackend.domain.auth.entity.RefreshToken;
import com.example.kxbackend.domain.auth.repository.RefreshTokenRepository;
import com.example.kxbackend.domain.user.entity.User;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import com.example.kxbackend.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * Refresh token 저장
     */
    @Transactional
    public void save(User user, String refreshToken) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .build();

        refreshTokenRepository.save(token);
    }

    /**
     * Refresh token 검증
     */
    public RefreshToken findValidToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (token.isExpired()) {
            throw new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        return token;
    }

    /**
     * Refresh token 교체
     */
    @Transactional
    public void rotate(String oldRefreshToken, User user, String newRefreshToken) {
        refreshTokenRepository.deleteByToken(oldRefreshToken);
        save(user, newRefreshToken);
    }

    /**
     * Refresh token 삭제
     */
    @Transactional
    public void deleteByToken(String refreshToken) {
        refreshTokenRepository.deleteByToken(refreshToken);
    }

}
