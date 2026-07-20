package com.example.kxbackend.domain.media.dto.response;

import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.media.entity.enums.MediaType;

import java.time.LocalDateTime;
import java.util.Set;

public record RecentMediaWorkItemResponseDto(
        Long mediaFileId,
        MediaType type,
        String filePath,
        String model,
        String quality,
        String aspectRatio,
        String resolution,
        boolean favorite,
        LocalDateTime createdAt
) {

    public static RecentMediaWorkItemResponseDto from(MediaFile mediaFile, Set<Long> favoriteMediaFileIds) {
        return new RecentMediaWorkItemResponseDto(
                mediaFile.getId(),
                mediaFile.getType(),
                mediaFile.getFilePath(),
                mediaFile.getModel(),
                mediaFile.getQuality(),
                mediaFile.getAspectRatio(),
                mediaFile.getResolution(),
                favoriteMediaFileIds.contains(mediaFile.getId()),
                mediaFile.getCreatedAt()
        );
    }
}
