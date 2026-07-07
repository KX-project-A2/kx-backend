package com.example.kxbackend.domain.job.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PromptKind {
    IMAGE("IMAGE", "이미지 생성용 프롬프트"),
    SCENE("SCENE", "영상 씬 설명 프롬프트");

    private final String key;
    private final String description;
}
