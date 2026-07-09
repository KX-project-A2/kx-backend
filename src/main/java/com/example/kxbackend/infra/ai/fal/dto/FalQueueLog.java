package com.example.kxbackend.infra.ai.fal.dto;

import com.example.kxbackend.domain.generate.client.dto.VideoGenerationLog;

public record FalQueueLog(
        String message,
        String timestamp
) {

    public VideoGenerationLog toResult() {
        return new VideoGenerationLog(message, timestamp);
    }
}
