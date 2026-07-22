package com.example.kxbackend.domain.generate.dto.response;

import com.example.kxbackend.domain.generate.entity.GenerateJob;
import com.example.kxbackend.domain.generate.entity.enums.Status;
import com.example.kxbackend.domain.generate.entity.enums.Type;
import com.example.kxbackend.domain.media.entity.MediaFile;

import java.time.LocalDateTime;

public record GenerateJobResponseDto(
        Long id,
        Type type,
        Status status,
        String falRequestId,
        String falModelId,
        String falStatusUrl,
        String falResponseUrl,
        Long inputMediaFileId,
        Long resultMediaFileId,
        LocalDateTime submittedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static GenerateJobResponseDto from(GenerateJob generateJob) {
        return from(generateJob, null);
    }

    public static GenerateJobResponseDto from(GenerateJob generateJob, MediaFile resultMediaFile) {
        MediaFile inputMediaFile = activeMediaFile(generateJob.getInputMediaFile());
        MediaFile activeResultMediaFile = activeMediaFile(resultMediaFile);
        return new GenerateJobResponseDto(
                generateJob.getId(),
                generateJob.getType(),
                generateJob.getStatus(),
                generateJob.getFalRequestId(),
                generateJob.getFalModelId(),
                generateJob.getFalStatusUrl(),
                generateJob.getFalResponseUrl(),
                inputMediaFile == null ? null : inputMediaFile.getId(),
                activeResultMediaFile == null ? null : activeResultMediaFile.getId(),
                generateJob.getSubmittedAt(),
                generateJob.getCompletedAt(),
                generateJob.getCreatedAt(),
                generateJob.getUpdatedAt()
        );
    }

    private static MediaFile activeMediaFile(MediaFile mediaFile) {
        if (mediaFile == null || mediaFile.isDeleted()) {
            return null;
        }
        return mediaFile;
    }
}
