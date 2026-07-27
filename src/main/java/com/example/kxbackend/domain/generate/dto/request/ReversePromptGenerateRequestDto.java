package com.example.kxbackend.domain.generate.dto.request;

import com.example.kxbackend.domain.generate.entity.enums.ImageGenerationPurpose;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 역프롬프트로 이미지 재생성 요청 Dto
 */
public record ReversePromptGenerateRequestDto(

        @NotNull(message = "이미지 생성 목적은 필수입니다.")
        ImageGenerationPurpose purpose,

        @Min(value = 1, message = "생성 이미지 수는 1장 이상이어야 합니다.")
        @Max(value = 4, message = "생성 이미지 수는 최대 4장까지 가능합니다.")
        Integer imageCount,

        /**
         * 미입력 시 역프롬프트에 저장된 aspectRatio를 size로 변환해 사용한다.
         */
        @Pattern(
                regexp = "^(auto|1024x1024|1536x864|864x1536)$",
                message = "지원하지 않는 이미지 크기입니다."
        )
        String size,

        @Pattern(regexp = "^(standard|high)$", message = "quality는 standard, high 중 하나여야 합니다.")
        String quality,

        Boolean promptCorrectionEnabled
) {
}
