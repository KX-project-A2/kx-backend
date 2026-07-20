package com.example.kxbackend.domain.generate.dto.response;

import com.example.kxbackend.domain.generate.client.dto.VideoGenerationLog;
import com.example.kxbackend.domain.generate.client.dto.VideoGenerationStatusResult;
import com.example.kxbackend.domain.generate.entity.GenerateJob;
import com.example.kxbackend.domain.generate.entity.enums.Status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record GenerateJobStatusResponseDto(
        Long id,
        Status status,
        String falRequestId,
        String falStatus,
        Integer queuePosition,
        String responseUrl,
        List<VideoGenerationLog> logs,
        Map<String, Object> metrics,
        Long resultMediaFileId,
        String resultFilePath,
        String resultModel,
        String resultQuality,
        String resultAspectRatio,
        String resultResolution,
        String errorMessage,
        LocalDateTime submittedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static GenerateJobStatusResponseDto from(GenerateJob generateJob, VideoGenerationStatusResult falStatus) {
        return new GenerateJobStatusResponseDto(
                generateJob.getId(),
                generateJob.getStatus(),
                generateJob.getFalRequestId(),
                falStatus == null ? null : falStatus.status(),
                falStatus == null ? null : falStatus.queuePosition(),
                falStatus == null ? null : falStatus.responseUrl(),
                falStatus == null ? List.of() : falStatus.logs(),
                falStatus == null ? null : falStatus.metrics(),
                generateJob.getResultMediaFile() == null ? null : generateJob.getResultMediaFile().getId(),
                generateJob.getResultMediaFile() == null ? null : generateJob.getResultMediaFile().getFilePath(),
                generateJob.getResultMediaFile() == null ? null : generateJob.getResultMediaFile().getModel(),
                generateJob.getResultMediaFile() == null ? null : generateJob.getResultMediaFile().getQuality(),
                generateJob.getResultMediaFile() == null ? null : generateJob.getResultMediaFile().getAspectRatio(),
                generateJob.getResultMediaFile() == null ? null : generateJob.getResultMediaFile().getResolution(),
                generateJob.getErrorMessage(),
                generateJob.getSubmittedAt(),
                generateJob.getCompletedAt(),
                generateJob.getCreatedAt(),
                generateJob.getUpdatedAt()
        );
    }
}
