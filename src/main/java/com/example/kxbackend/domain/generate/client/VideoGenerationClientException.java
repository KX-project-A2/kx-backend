package com.example.kxbackend.domain.generate.client;

import lombok.Getter;

@Getter
public class VideoGenerationClientException extends RuntimeException {

    private final int statusCode;
    private final String responseBody;

    public VideoGenerationClientException(int statusCode, String responseBody) {
        super("영상 생성 API 호출에 실패했습니다. statusCode=" + statusCode);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
}
