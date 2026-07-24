package com.example.kxbackend.domain.auth.dto.response;

/**
 * 이메일 중복 확인 응답 Dto
 */
public record EmailCheckResponseDto(
        String email,
        boolean duplicated,
        boolean available
) {

    public static EmailCheckResponseDto of(String email, boolean duplicated) {
        return new EmailCheckResponseDto(email, duplicated, !duplicated);
    }
}
