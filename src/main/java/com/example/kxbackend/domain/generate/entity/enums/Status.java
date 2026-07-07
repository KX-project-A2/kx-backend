package com.example.kxbackend.domain.generate.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Status {
    CREATED("CREATED", "작업 생성"),
    SUBMITTED("SUBMITTED", "외부 생성 요청 제출"),
    IN_PROGRESS("IN_PROGRESS", "작업 진행 중"),
    COMPLETED("COMPLETED", "작업 완료"),
    FAILED("FAILED", "작업 실패"),
    CANCELED("CANCELED", "작업 취소");

    private final String key;
    private final String description;
}
