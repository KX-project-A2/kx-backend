package com.example.kxbackend.domain.share.dto.response;

import com.example.kxbackend.domain.media.dto.response.MediaFileResponseDto;
import com.example.kxbackend.domain.share.entity.ShareLink;

import java.time.LocalDateTime;

public record SharedMediaResponseDto(
        String token,
        LocalDateTime expiresAt,
        MediaFileResponseDto mediaFile
) {

    public static SharedMediaResponseDto from(ShareLink shareLink) {
        return new SharedMediaResponseDto(
                shareLink.getToken(),
                shareLink.getExpiresAt(),
                MediaFileResponseDto.from(shareLink.getMediaFile())
        );
    }
}
