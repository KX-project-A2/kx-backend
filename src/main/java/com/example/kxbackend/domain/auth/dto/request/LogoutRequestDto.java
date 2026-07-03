package com.example.kxbackend.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그아웃 요청 Dto
 */
public record LogoutRequestDto(

        @NotBlank(message = "refresh token은 필수입니다.")
        String refreshToken
) {
}
