package com.example.kxbackend.domain.generate.dto.response;

import com.example.kxbackend.domain.generate.entity.GenerateJob;
import com.example.kxbackend.domain.generate.entity.enums.Status;
import com.example.kxbackend.domain.generate.entity.enums.Type;

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
        return new GenerateJobResponseDto(
                generateJob.getId(),
                generateJob.getType(),
                generateJob.getStatus(),
                generateJob.getFalRequestId(),
                generateJob.getFalModelId(),
                generateJob.getFalStatusUrl(),
                generateJob.getFalResponseUrl(),
                generateJob.getInputMediaFile() == null ? null : generateJob.getInputMediaFile().getId(),
                generateJob.getResultMediaFile() == null ? null : generateJob.getResultMediaFile().getId(),
                generateJob.getSubmittedAt(),
                generateJob.getCompletedAt(),
                generateJob.getCreatedAt(),
                generateJob.getUpdatedAt()
        );
    }
}
