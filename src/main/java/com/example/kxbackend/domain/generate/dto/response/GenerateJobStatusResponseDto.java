package com.example.kxbackend.domain.generate.dto.response;

import com.example.kxbackend.domain.generate.client.dto.VideoGenerationLog;
import com.example.kxbackend.domain.generate.client.dto.VideoGenerationStatusResult;
import com.example.kxbackend.domain.generate.entity.GenerateJob;
import com.example.kxbackend.domain.generate.entity.enums.Status;
import com.example.kxbackend.domain.media.entity.MediaFile;

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
        MediaFile resultMediaFile = activeResultMediaFile(generateJob);
        return new GenerateJobStatusResponseDto(
                generateJob.getId(),
                generateJob.getStatus(),
                generateJob.getFalRequestId(),
                falStatus == null ? null : falStatus.status(),
                falStatus == null ? null : falStatus.queuePosition(),
                falStatus == null ? null : falStatus.responseUrl(),
                falStatus == null ? List.of() : falStatus.logs(),
                falStatus == null ? null : falStatus.metrics(),
                resultMediaFile == null ? null : resultMediaFile.getId(),
                resultMediaFile == null ? null : resultMediaFile.getFilePath(),
                resultMediaFile == null ? null : resultMediaFile.getModel(),
                resultMediaFile == null ? null : resultMediaFile.getQuality(),
                resultMediaFile == null ? null : resultMediaFile.getAspectRatio(),
                resultMediaFile == null ? null : resultMediaFile.getResolution(),
                generateJob.getErrorMessage(),
                generateJob.getSubmittedAt(),
                generateJob.getCompletedAt(),
                generateJob.getCreatedAt(),
                generateJob.getUpdatedAt()
        );
    }

    private static MediaFile activeResultMediaFile(GenerateJob generateJob) {
        MediaFile resultMediaFile = generateJob.getResultMediaFile();
        if (resultMediaFile == null || resultMediaFile.isDeleted()) {
            return null;
        }
        return resultMediaFile;
    }
}
