package com.example.kxbackend.infra.ai.fal.dto;

import com.example.kxbackend.domain.generate.client.dto.VideoGenerationStatusResult;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record FalQueueStatusResponse(
        String status,

        @JsonProperty("request_id")
        String requestId,

        @JsonProperty("queue_position")
        Integer queuePosition,

        @JsonProperty("response_url")
        String responseUrl,

        List<FalQueueLog> logs,
        Map<String, Object> metrics,
        String error,

        @JsonProperty("error_type")
        String errorType
) {

    public VideoGenerationStatusResult toResult() {
        return new VideoGenerationStatusResult(
                status,
                requestId,
                queuePosition,
                responseUrl,
                logs == null ? List.of() : logs.stream().map(FalQueueLog::toResult).toList(),
                metrics,
                error,
                errorType
        );
    }
}
