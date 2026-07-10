package com.example.kxbackend.domain.media.dto.response;

import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.media.entity.enums.MediaType;

import java.time.LocalDateTime;

/**
 * 이미지 업로드 응답 Dto
 */
public record MediaImageUploadResponseDto(
        Long mediaFileId,
        MediaType type,
        String filePath,
        String tags,
        LocalDateTime createdAt
) {

    public static MediaImageUploadResponseDto from(MediaFile mediaFile) {
        return new MediaImageUploadResponseDto(
                mediaFile.getId(),
                mediaFile.getType(),
                mediaFile.getFilePath(),
                mediaFile.getTags(),
                mediaFile.getCreatedAt()
        );
    }
}
