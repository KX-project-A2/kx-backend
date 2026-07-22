package com.example.kxbackend.domain.generate.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 이미지 역프롬프트 추출 요청 Dto
 */
public record ReversePromptRequestDto(

        Long mediaFileId,

        @NotBlank(message = "생성 이미지 비율은 필수입니다.")
        @Pattern(
                regexp = "^(auto|1:1|16:9|9:16)$",
                message = "지원하지 않는 이미지 비율입니다."
        )
        String aspectRatio
) {
}
