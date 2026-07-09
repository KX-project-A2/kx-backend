package com.example.kxbackend.domain.generate.dto.response;

import com.example.kxbackend.domain.generate.client.dto.VideoGenerationLog;
import com.example.kxbackend.domain.generate.client.dto.VideoGenerationStatusResult;
import com.example.kxbackend.domain.generate.entity.GenerateJob;
import com.example.kxbackend.domain.generate.entity.enums.Status;
import com.example.kxbackend.domain.generate.entity.enums.Type;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record GenerateJobStatusResponseDto(
        Long id,
        Type type,
        Status status,
        String falRequestId,
        String falModelId,
        String falStatusUrl,
        String falResponseUrl,
        Long inputMediaFileId,
        String inputFilePath,
        Long resultMediaFileId,
        String resultFilePath,
        String errorMessage,
        String falStatus,
        Integer queuePosition,
        String responseUrl,
        List<VideoGenerationLog> logs,
        Map<String, Object> metrics,
        String falError,
        String falErrorType,
        Integer progressPercent,
        LocalDateTime submittedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static GenerateJobStatusResponseDto from(GenerateJob generateJob, VideoGenerationStatusResult falStatus) {
        return new GenerateJobStatusResponseDto(
                generateJob.getId(),
                generateJob.getType(),
                generateJob.getStatus(),
                generateJob.getFalRequestId(),
                generateJob.getFalModelId(),
                generateJob.getFalStatusUrl(),
                generateJob.getFalResponseUrl(),
                generateJob.getInputMediaFile() == null ? null : generateJob.getInputMediaFile().getId(),
                generateJob.getInputMediaFile() == null ? null : generateJob.getInputMediaFile().getFilePath(),
                generateJob.getResultMediaFile() == null ? null : generateJob.getResultMediaFile().getId(),
                generateJob.getResultMediaFile() == null ? null : generateJob.getResultMediaFile().getFilePath(),
                generateJob.getErrorMessage(),
                falStatus == null ? null : falStatus.status(),
                falStatus == null ? null : falStatus.queuePosition(),
                falStatus == null ? null : falStatus.responseUrl(),
                falStatus == null ? List.of() : falStatus.logs(),
                falStatus == null ? null : falStatus.metrics(),
                falStatus == null ? null : falStatus.error(),
                falStatus == null ? null : falStatus.errorType(),
                calculateProgressPercent(generateJob, falStatus),
                generateJob.getSubmittedAt(),
                generateJob.getCompletedAt(),
                generateJob.getCreatedAt(),
                generateJob.getUpdatedAt()
        );
    }

    private static Integer calculateProgressPercent(GenerateJob generateJob, VideoGenerationStatusResult falStatus) {
        if (generateJob.getStatus() == Status.COMPLETED) {
            return 100;
        }
        if (generateJob.getStatus() == Status.FAILED || generateJob.getStatus() == Status.CANCELED) {
            return 0;
        }
        if (falStatus == null || falStatus.status() == null) {
            return generateJob.getStatus() == Status.SUBMITTED ? 10 : 0;
        }
        if ("COMPLETED".equalsIgnoreCase(falStatus.status())) {
            return 100;
        }
        if ("IN_PROGRESS".equalsIgnoreCase(falStatus.status())) {
            return 50;
        }
        if ("IN_QUEUE".equalsIgnoreCase(falStatus.status())) {
            return 20;
        }
        return generateJob.getStatus() == Status.SUBMITTED ? 10 : null;
    }
}
