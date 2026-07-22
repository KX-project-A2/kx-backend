package com.example.kxbackend.domain.generate.dto.response;

import com.example.kxbackend.domain.generate.entity.GenerateJob;
import com.example.kxbackend.domain.generate.entity.OpenAiImageReference;
import com.example.kxbackend.domain.generate.entity.OpenAiImageGenerateJobOption;
import com.example.kxbackend.domain.generate.entity.enums.ImageGenerationPurpose;
import com.example.kxbackend.domain.generate.entity.enums.Status;
import com.example.kxbackend.domain.generate.entity.enums.Type;
import com.example.kxbackend.domain.media.entity.MediaFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * OpenAI 이미지 생성 작업 응답 Dto
 */
public record OpenAiGenerateImageJobResponseDto(
        Long jobId,
        Type type,
        Status status,
        String batchId,
        String prompt,
        Integer imageCount,
        String size,
        String quality,
        ImageGenerationPurpose purpose,
        Long resultMediaFileId,
        String resultFilePath,
        List<OpenAiGeneratedImageResultDto> resultImages,
        List<OpenAiReferenceImageResultDto> referenceImages,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime submittedAt,
        LocalDateTime completedAt
) {

    public static OpenAiGenerateImageJobResponseDto from(
            GenerateJob job,
            OpenAiImageGenerateJobOption option,
            List<MediaFile> resultMediaFiles,
            List<OpenAiImageReference> references
    ) {
        String prompt = job.getPrompts().isEmpty() ? null : job.getPrompts().getFirst().getContent();
        List<OpenAiGeneratedImageResultDto> resultImages = resultMediaFiles.stream()
                .map(mediaFile -> new OpenAiGeneratedImageResultDto(mediaFile.getId(), mediaFile.getFilePath()))
                .toList();
        List<OpenAiReferenceImageResultDto> referenceImages = references.stream()
                .map(OpenAiReferenceImageResultDto::from)
                .toList();

        Long resultMediaFileId = resultImages.isEmpty() ? null : resultImages.getFirst().mediaFileId();
        String resultFilePath = resultImages.isEmpty() ? null : resultImages.getFirst().filePath();

        return new OpenAiGenerateImageJobResponseDto(
                job.getId(),
                job.getType(),
                job.getStatus(),
                job.getFalRequestId(),
                prompt,
                option != null ? option.getImageCount() : null,
                option != null ? option.getSize() : null,
                option != null ? option.getQuality() : null,
                option != null ? option.getPurpose() : ImageGenerationPurpose.CHARACTER,
                resultMediaFileId,
                resultFilePath,
                resultImages,
                referenceImages,
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getSubmittedAt(),
                job.getCompletedAt()
        );
    }
}
