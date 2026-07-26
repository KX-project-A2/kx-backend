package com.example.kxbackend.domain.generate.dto.response;

import com.example.kxbackend.domain.generate.entity.GenerateJob;
import com.example.kxbackend.domain.generate.entity.OpenAiImageGenerateJobOption;
import com.example.kxbackend.domain.generate.entity.OpenAiImageReference;
import com.example.kxbackend.domain.generate.entity.enums.ImageGenerationPurpose;
import com.example.kxbackend.domain.generate.entity.enums.Status;
import com.example.kxbackend.domain.generate.entity.enums.Type;

import java.time.LocalDateTime;
import java.util.List;

public record OpenAiActiveImageJobResponseDto(
        Long jobId,
        Type type,
        Status status,
        String prompt,
        Integer imageCount,
        String size,
        String quality,
        ImageGenerationPurpose purpose,
        List<OpenAiReferenceImageResultDto> referenceImages,
        LocalDateTime createdAt,
        LocalDateTime submittedAt
) {

    public static OpenAiActiveImageJobResponseDto from(
            GenerateJob job,
            OpenAiImageGenerateJobOption option,
            List<OpenAiImageReference> references
    ) {
        return new OpenAiActiveImageJobResponseDto(
                job.getId(),
                job.getType(),
                job.getStatus(),
                job.getPrompts().isEmpty() ? null : job.getPrompts().getFirst().getContent(),
                option != null ? option.getImageCount() : null,
                option != null ? option.getSize() : null,
                option != null ? option.getQuality() : null,
                option != null ? option.getPurpose() : ImageGenerationPurpose.CHARACTER,
                references.stream()
                        .map(OpenAiReferenceImageResultDto::from)
                        .toList(),
                job.getCreatedAt(),
                job.getSubmittedAt()
        );
    }
}
