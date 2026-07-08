package com.example.kxbackend.domain.generate.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ImageToVideoGenerateRequestDto(

        @NotNull(message = "입력 이미지 파일 ID는 필수입니다.")
        Long inputMediaFileId,

        @NotBlank(message = "fal.ai 모델 ID는 필수입니다.")
        String modelId,

        @NotBlank(message = "프롬프트는 필수입니다.")
        String prompt,

        String webhookUrl,

        Map<String, Object> options
) {
}
