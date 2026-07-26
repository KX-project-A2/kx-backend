package com.example.kxbackend.domain.generate.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 역프롬프트 편집 요청 Dto
 */
public record ReversePromptUpdateRequestDto(

        @Size(max = 4000, message = "프롬프트는 4000자 이하여야 합니다.")
        String prompt,

        @Pattern(
                regexp = "^(auto|1:1|16:9|9:16)$",
                message = "지원하지 않는 이미지 비율입니다."
        )
        String aspectRatio
) {
}
