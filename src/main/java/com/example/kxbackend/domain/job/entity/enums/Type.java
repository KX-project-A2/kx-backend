package com.example.kxbackend.domain.job.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Type {
    IMAGE("IMAGE", "이미지"),
    VIDEO("VIDEO", "영상"),
    PROMPT("PROMPT", "프롬프트");

    private final String key;
    private final String description;
}