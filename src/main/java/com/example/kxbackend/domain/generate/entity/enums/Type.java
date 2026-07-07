package com.example.kxbackend.domain.generate.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Type {
    TEXT_TO_IMAGE("TEXT_TO_IMAGE", "프롬프트 기반 이미지 생성"),
    IMAGE_TO_VIDEO("IMAGE_TO_VIDEO", "이미지 기반 영상 생성");

    private final String key;
    private final String description;
}
