package com.example.kxbackend.domain.generate.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * OpenAI 이미지 생성 요청 Dto
 */
public record OpenAiGenerateImageRequestDto(

        @NotBlank(message = "프롬프트는 필수입니다.")
        @Size(max = 2000, message = "프롬프트는 2000자 이하여야 합니다.")
        String prompt,

        @Min(value = 1, message = "생성 이미지 수는 1장 이상이어야 합니다.")
        @Max(value = 4, message = "생성 이미지 수는 최대 4장까지 가능합니다.")
        Integer imageCount,

        @Pattern(
                regexp = "^(1024x1024|1024x1536|1536x1024|1792x1024|1024x1792|2048x2048)$",
                message = "지원하지 않는 이미지 크기입니다."
        )
        String size,

        @Pattern(regexp = "^(low|medium|high)$", message = "quality는 low, medium, high 중 하나여야 합니다.")
        String quality
) {
}
