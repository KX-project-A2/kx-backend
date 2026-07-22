package com.example.kxbackend.domain.generate.dto.response;

import com.example.kxbackend.domain.generate.entity.OpenAiImageReference;

/**
 * 이미지 생성에 사용된 레퍼런스 이미지 응답 Dto
 */
public record OpenAiReferenceImageResultDto(
        Long mediaFileId,
        int referenceOrder,
        String filePath
) {

    public static OpenAiReferenceImageResultDto from(OpenAiImageReference reference) {
        return new OpenAiReferenceImageResultDto(
                reference.getMediaFile().getId(),
                reference.getReferenceOrder(),
                reference.getMediaFile().getFilePath()
        );
    }
}
