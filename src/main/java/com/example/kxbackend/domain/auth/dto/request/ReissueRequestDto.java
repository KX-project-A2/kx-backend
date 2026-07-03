package com.example.kxbackend.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 토큰 재발급 요청 Dto
 */
public record ReissueRequestDto(

        @NotBlank(message = "refresh token은 필수입니다.")
        String refreshToken
) {
}
