package com.example.kxbackend.domain.media.dto.response;

public record MediaFileDownloadUrlResponseDto(
        String downloadUrl,
        long expiresInSeconds
) {
}
