package com.example.kxbackend.domain.job.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Status {
    PENDING("PENDING", "작업 대기 중"),
    RUNNING("RUNNING", "작업 진행 중"),
    SUCCESS("SUCCESS", "작업 완료"),
    FAILED("FAILED", "작업 실패");

    private final String key;
    private final String description;
}
