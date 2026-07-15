package com.example.kxbackend.domain.auth.dto.response;

import com.example.kxbackend.domain.user.entity.User;
import com.example.kxbackend.domain.user.entity.enums.Role;

import java.time.LocalDateTime;

/**
 * 회원가입 응답 Dto
 * - 생성된 사용자 정보를 포함한다.
 */
public record SignUpResponseDto(
        Long id,
        String email,
        String nickname,
        Role role,
        LocalDateTime createdAt
) {

    /**
     * User 엔티티로 SignUpResponseDto 생성
     */
    public static SignUpResponseDto of(User user) {
        return new SignUpResponseDto(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
