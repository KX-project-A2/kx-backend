package com.example.kxbackend.domain.generate.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record ImageToVideoGenerateRequestDto(

        Long startMediaFileId,

        Long endMediaFileId,

        @Size(max = 4, message = "참조 이미지는 최대 4장까지 사용할 수 있습니다.")
        List<Long> referenceMediaFileIds,

        String modelId,

        @NotBlank(message = "프롬프트는 필수입니다.")
        @Size(max = 2500, message = "프롬프트는 2500자 이하여야 합니다.")
        String prompt,

        String webhookUrl,

        Map<String, Object> options
) {
}
