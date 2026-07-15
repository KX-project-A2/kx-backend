package com.example.kxbackend.domain.media.dto.response;

import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.media.entity.enums.MediaType;

import java.time.LocalDateTime;

public record MediaFileResponseDto(
        Long id,
        MediaType type,
        String filePath,
        String reversedPrompt,
        String tags,
        Long generateJobId,
        Long generatePromptId,
        LocalDateTime createdAt
) {

    public static MediaFileResponseDto from(MediaFile mediaFile) {
        return new MediaFileResponseDto(
                mediaFile.getId(),
                mediaFile.getType(),
                mediaFile.getFilePath(),
                mediaFile.getReversedPrompt(),
                mediaFile.getTags(),
                mediaFile.getGenerateJob() == null ? null : mediaFile.getGenerateJob().getId(),
                mediaFile.getGeneratePrompt() == null ? null : mediaFile.getGeneratePrompt().getId(),
                mediaFile.getCreatedAt()
        );
    }
}
