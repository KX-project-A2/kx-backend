package com.example.kxbackend.domain.auth.dto.response;

/**
 * JWT 토큰 응답 Dto
 * - access token: API 요청 시 Authorization 헤더에 사용
 * - refresh token: access token 만료 시 재발급에 사용
 */
public record TokenResponseDto(
        String accessToken,
        String refreshToken
) {
}
