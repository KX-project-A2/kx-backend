package com.example.kxbackend.domain.generate.dto.response;

/**
 * 이미지 역프롬프트 추출 응답 Dto
 */
public record ReversePromptResponseDto(
        String prompt,
        String aspectRatio,
        Long mediaFileId
) {
}
