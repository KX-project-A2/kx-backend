package com.example.kxbackend.domain.generate.client.dto;

public record VideoGenerationSubmitResult(
        String requestId,
        String responseUrl,
        String statusUrl,
        String cancelUrl,
        Integer queuePosition
) {
}
