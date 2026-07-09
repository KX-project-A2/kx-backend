package com.example.kxbackend.domain.generate.client.dto;

import java.util.List;
import java.util.Map;

public record VideoGenerationStatusResult(
        String status,
        String requestId,
        Integer queuePosition,
        String responseUrl,
        List<VideoGenerationLog> logs,
        Map<String, Object> metrics,
        String error,
        String errorType
) {
}
