package com.example.kxbackend.domain.generate.entity.enums;

/**
 * 이미지 생성 레퍼런스 유형
 */
public enum ReferenceImageType {
    REFERENCE,
    /** 하위 호환용. 신규 저장에는 사용하지 않는다. */
    @Deprecated
    STYLE,
    /** 하위 호환용. 신규 저장에는 사용하지 않는다. */
    @Deprecated
    CHARACTER
}
