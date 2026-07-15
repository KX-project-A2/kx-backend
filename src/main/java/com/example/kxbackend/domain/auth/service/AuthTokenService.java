package com.example.kxbackend.domain.auth.service;

import com.example.kxbackend.domain.auth.dto.response.TokenResponseDto;
import com.example.kxbackend.domain.user.entity.User;
import com.example.kxbackend.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthTokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    /**
     * Access token과 refresh token을 발급하고 refresh token을 저장한다.
     */
    @Transactional
    public TokenResponseDto issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        refreshTokenService.save(user, refreshToken);
        return new TokenResponseDto(accessToken, refreshToken);
    }

    /**
     * 기존 refresh token을 새 refresh token으로 교체한다.
     */
    @Transactional
    public TokenResponseDto rotateTokens(String oldRefreshToken, User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        refreshTokenService.rotate(oldRefreshToken, user, refreshToken);
        return new TokenResponseDto(accessToken, refreshToken);
    }
}
