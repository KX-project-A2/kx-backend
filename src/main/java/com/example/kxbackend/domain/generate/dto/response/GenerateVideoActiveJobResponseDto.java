package com.example.kxbackend.domain.generate.dto.response;

import com.example.kxbackend.domain.generate.entity.GenerateJob;
import com.example.kxbackend.domain.generate.entity.GenerateJobReferenceMedia;
import com.example.kxbackend.domain.generate.entity.enums.Status;
import com.example.kxbackend.domain.generate.entity.enums.Type;
import com.example.kxbackend.domain.media.entity.MediaFile;

import java.time.LocalDateTime;
import java.util.List;

public record GenerateVideoActiveJobResponseDto(
        Long id,
        Type type,
        Status status,
        String prompt,
        String falRequestId,
        String falModelId,
        String falStatusUrl,
        String falResponseUrl,
        String requestQuality,
        String requestAspectRatio,
        String requestResolution,
        Long inputMediaFileId,
        List<GenerateJobReferenceMediaResponseDto> referenceMedia,
        String errorMessage,
        LocalDateTime submittedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static GenerateVideoActiveJobResponseDto from(
            GenerateJob generateJob,
            List<GenerateJobReferenceMedia> referenceMedia
    ) {
        MediaFile inputMediaFile = activeMediaFile(generateJob.getInputMediaFile());
        return new GenerateVideoActiveJobResponseDto(
                generateJob.getId(),
                generateJob.getType(),
                generateJob.getStatus(),
                generateJob.getPrompts().isEmpty() ? null : generateJob.getPrompts().getFirst().getContent(),
                generateJob.getFalRequestId(),
                generateJob.getFalModelId(),
                generateJob.getFalStatusUrl(),
                generateJob.getFalResponseUrl(),
                generateJob.getRequestQuality(),
                generateJob.getRequestAspectRatio(),
                generateJob.getRequestResolution(),
                inputMediaFile == null ? null : inputMediaFile.getId(),
                referenceMedia.stream()
                        .filter(reference -> activeMediaFile(reference.getMediaFile()) != null)
                        .map(GenerateJobReferenceMediaResponseDto::from)
                        .toList(),
                generateJob.getErrorMessage(),
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
