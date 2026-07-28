package com.example.kxbackend.domain.generate.dto.request;

import com.example.kxbackend.domain.generate.entity.enums.ImageGenerationPurpose;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * OpenAI 이미지 생성 요청 Dto
 */
public record OpenAiGenerateImageRequestDto(

        @NotBlank(message = "프롬프트는 필수입니다.")
        @Size(max = 2000, message = "프롬프트는 2000자 이하여야 합니다.")
        String prompt,

        @NotNull(message = "이미지 생성 목적은 필수입니다.")
        ImageGenerationPurpose purpose,

        @Min(value = 1, message = "생성 이미지 수는 1장 이상이어야 합니다.")
        @Max(value = 4, message = "생성 이미지 수는 최대 4장까지 가능합니다.")
        Integer imageCount,

        @Pattern(
                regexp = "^(auto|1024x1024|1536x864|864x1536)$",
                message = "지원하지 않는 이미지 크기입니다."
        )
        String size,

        @Pattern(regexp = "^(standard|high)$", message = "quality는 standard, high 중 하나여야 합니다.")
        String quality,

        Boolean promptCorrectionEnabled,

        /**
         * CHARACTER일 때 정면/측면/후면 다각도 배치 여부.
         * null이면 true(기존 동작)로 처리한다. BACKGROUND에서는 무시된다.
         */
        Boolean multiViewEnabled
) {
}
