package com.example.kxbackend.domain.generate.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 공식 캐릭터 설정표(Concept Art Sheet) 생성 요청 Dto
 * UI 선택 속성(성별, 체형, 아트 스타일, 헤어, 의상 등)만 수신한다.
 */
public record CharacterConceptSheetRequestDto(

        @Size(max = 100)
        String gender,

        @Size(max = 50)
        String age,

        @Size(max = 100)
        String bodyType,

        @Size(max = 100)
        String artStyle,

        @Size(max = 100)
        String worldView,

        @Size(max = 100)
        String hairLength,

        @Size(max = 100)
        String hairStyle,

        @Size(max = 100)
        String hairColor,

        @Size(max = 100)
        String eyeColor,

        @Size(max = 100)
        String expression,

        @Size(max = 100)
        String outfitGenre,

        @Size(max = 100)
        String outfitColor,

        /** 복수 선택 가능한 악세서리. "없음"은 다른 아이템과 함께 오면 무시된다. */
        List<@Size(max = 100) String> accessories,

        @Min(value = 1, message = "생성 이미지 수는 1장 이상이어야 합니다.")
        @Max(value = 4, message = "생성 이미지 수는 최대 4장까지 가능합니다.")
        Integer imageCount,

        /**
         * 이미지 비율/크기.
         * Auto는 null 또는 "auto"로 전달하면 설정표 기본값(1536x1024)을 사용한다.
         */
        @Pattern(
                regexp = "^(auto|1024x1024|1024x1536|1536x1024|1792x1024|1024x1792|2048x2048)$",
                message = "지원하지 않는 이미지 크기입니다."
        )
        String size,

        @Pattern(regexp = "^(low|medium|high)$", message = "quality는 low, medium, high 중 하나여야 합니다.")
        String quality
) {
}
