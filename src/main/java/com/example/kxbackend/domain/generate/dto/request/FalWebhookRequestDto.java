package com.example.kxbackend.domain.generate.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record FalWebhookRequestDto(

        @JsonProperty("request_id")
        String requestId,

        String status,

        Map<String, Object> payload,

        String error
) {
}
