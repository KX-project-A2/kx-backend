package com.example.kxbackend.domain.user.dto.response;

import com.example.kxbackend.domain.user.entity.User;

public record ProfileResponseDto(
        Long id,
        String email,
        String nickname,
        String profileImageUrl
) {

    public static ProfileResponseDto from(User user, String profileImageUrl) {
        return new ProfileResponseDto(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                profileImageUrl
        );
    }
}
