package com.example.kxbackend.domain.media.dto.response;

import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.media.entity.enums.MediaType;

import java.time.LocalDateTime;
import java.util.Set;

public record MediaFileResponseDto(
        Long id,
        MediaType type,
        String filePath,
        String model,
        String quality,
        String aspectRatio,
        String resolution,
        Long reversedPromptId,
        String tags,
        Long generateJobId,
        Long generatePromptId,
        String generatePromptContent,
        boolean favorite,
        LocalDateTime createdAt
) {

    public static MediaFileResponseDto from(MediaFile mediaFile) {
        return from(mediaFile, false);
    }

    public static MediaFileResponseDto from(MediaFile mediaFile, boolean favorite) {
        return new MediaFileResponseDto(
                mediaFile.getId(),
                mediaFile.getType(),
                mediaFile.getFilePath(),
                mediaFile.getModel(),
                mediaFile.getQuality(),
                mediaFile.getAspectRatio(),
                mediaFile.getResolution(),
                mediaFile.getReversedPrompt() == null ? null : mediaFile.getReversedPrompt().getId(),
                mediaFile.getTags(),
                mediaFile.getGenerateJob() == null ? null : mediaFile.getGenerateJob().getId(),
                mediaFile.getGeneratePrompt() == null ? null : mediaFile.getGeneratePrompt().getId(),
                mediaFile.getGeneratePrompt() == null ? null : mediaFile.getGeneratePrompt().getContent(),
                favorite,
                mediaFile.getCreatedAt()
        );
    }

    public static MediaFileResponseDto from(MediaFile mediaFile, Set<Long> favoriteMediaFileIds) {
        return from(mediaFile, favoriteMediaFileIds.contains(mediaFile.getId()));
    }
}
