package com.example.kxbackend.domain.auth.dto.response;

import com.example.kxbackend.domain.user.entity.User;
import com.example.kxbackend.domain.user.entity.enums.Role;

import java.time.LocalDateTime;

/**
 * 로그인 응답 Dto
 * - 사용자 정보를 포함한다.
 */
public record LoginResponseDto(
        Long id,
        String email,
        String nickname,
        Role role,
        LocalDateTime createdAt
) {

    /**
     * User 엔티티로 LoginResponseDto 생성
     */
    public static LoginResponseDto of(User user) {
        return new LoginResponseDto(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
