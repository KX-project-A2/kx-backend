package com.example.kxbackend.domain.generate.client.dto;

import java.util.Map;

public record VideoGenerationCommand(
        String modelId,
        Map<String, Object> input,
        String webhookUrl
) {
}
