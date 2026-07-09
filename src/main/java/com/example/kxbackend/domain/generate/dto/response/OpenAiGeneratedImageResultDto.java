package com.example.kxbackend.domain.generate.dto.response;

/**
 * OpenAI 이미지 생성 결과 파일 Dto
 */
public record OpenAiGeneratedImageResultDto(
        Long mediaFileId,
        String filePath
) {
}
