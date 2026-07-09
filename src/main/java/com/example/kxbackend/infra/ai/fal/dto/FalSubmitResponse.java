package com.example.kxbackend.infra.ai.fal.dto;

import com.example.kxbackend.domain.generate.client.dto.VideoGenerationSubmitResult;
import com.fasterxml.jackson.annotation.JsonProperty;

public record FalSubmitResponse(
        @JsonProperty("request_id")
        String requestId,

        @JsonProperty("response_url")
        String responseUrl,

        @JsonProperty("status_url")
        String statusUrl,

        @JsonProperty("cancel_url")
        String cancelUrl,

        @JsonProperty("queue_position")
        Integer queuePosition
) {

    public VideoGenerationSubmitResult toResult() {
        return new VideoGenerationSubmitResult(
                requestId,
                responseUrl,
                statusUrl,
                cancelUrl,
                queuePosition
        );
    }
}
