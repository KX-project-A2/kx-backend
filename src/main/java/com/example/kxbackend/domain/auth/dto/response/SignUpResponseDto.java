package com.example.kxbackend.domain.auth.dto.response;

import com.example.kxbackend.domain.user.entity.User;
import com.example.kxbackend.domain.user.entity.enums.Role;

import java.time.LocalDateTime;

/**
 * 회원가입 응답 Dto
 * - 생성된 사용자 정보와 발급된 JWT 토큰을 포함한다.
 */
public record SignUpResponseDto(
        Long id,
        String email,
        String nickname,
        Role role,
        LocalDateTime createdAt,
        TokenResponseDto token
) {

    /**
     * User 엔티티와 TokenResponseDto로 SignUpResponseDto 생성
     */
    public static SignUpResponseDto of(User user, TokenResponseDto token) {
        return new SignUpResponseDto(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getCreatedAt(),
                token
        );
    }
}
