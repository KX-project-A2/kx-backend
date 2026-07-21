package com.example.kxbackend.domain.generate.dto.request;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record ImageToVideoGenerateRequestDto(

        Long startMediaFileId,

        Long endMediaFileId,

        @Size(max = 9, message = "참조 이미지는 최대 9장까지 사용할 수 있습니다.")
        List<Long> referenceMediaFileIds,

        String modelId,

        String prompt,

        String webhookUrl,

        Map<String, Object> options
) {
}
