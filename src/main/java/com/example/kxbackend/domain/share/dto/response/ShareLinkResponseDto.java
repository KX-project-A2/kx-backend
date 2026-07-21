package com.example.kxbackend.domain.share.dto.response;

import com.example.kxbackend.domain.share.entity.ShareLink;

import java.time.LocalDateTime;

public record ShareLinkResponseDto(
        Long id,
        Long mediaFileId,
        String token,
        String sharePath,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {

    public static ShareLinkResponseDto from(ShareLink shareLink) {
        return new ShareLinkResponseDto(
                shareLink.getId(),
                shareLink.getMediaFile().getId(),
                shareLink.getToken(),
                "/api/share/" + shareLink.getToken(),
                shareLink.getExpiresAt(),
                shareLink.getCreatedAt()
        );
    }
}
