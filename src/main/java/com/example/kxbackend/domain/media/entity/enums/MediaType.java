package com.example.kxbackend.domain.media.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MediaType {
    IMAGE("IMAGE", "이미지"),
    VIDEO("VIDEO", "영상");

    private final String key;
    private final String description;
}
