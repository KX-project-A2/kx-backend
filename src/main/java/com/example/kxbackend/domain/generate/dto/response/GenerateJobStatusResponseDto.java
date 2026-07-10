package com.example.kxbackend.domain.generate.dto.response;

import com.example.kxbackend.domain.generate.client.dto.VideoGenerationLog;
import com.example.kxbackend.domain.generate.client.dto.VideoGenerationStatusResult;
import com.example.kxbackend.domain.generate.entity.GenerateJob;
import com.example.kxbackend.domain.generate.entity.enums.Status;

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
        Integer progressPercent,
        Long resultMediaFileId,
        String resultFilePath,
        String errorMessage
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
                calculateProgressPercent(generateJob, falStatus),
                generateJob.getResultMediaFile() == null ? null : generateJob.getResultMediaFile().getId(),
                generateJob.getResultMediaFile() == null ? null : generateJob.getResultMediaFile().getFilePath(),
                generateJob.getErrorMessage()
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
