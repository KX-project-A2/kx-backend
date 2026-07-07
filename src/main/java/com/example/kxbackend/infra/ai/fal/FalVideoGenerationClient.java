package com.example.kxbackend.infra.ai.fal;

import com.example.kxbackend.domain.generate.client.VideoGenerationClient;
import com.example.kxbackend.domain.generate.client.dto.VideoGenerationCommand;
import com.example.kxbackend.domain.generate.client.dto.VideoGenerationResult;
import com.example.kxbackend.domain.generate.client.dto.VideoGenerationStatusResult;
import com.example.kxbackend.domain.generate.client.dto.VideoGenerationSubmitResult;
import com.example.kxbackend.infra.ai.fal.dto.FalQueueStatusResponse;
import com.example.kxbackend.infra.ai.fal.dto.FalSubmitResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class FalVideoGenerationClient implements VideoGenerationClient {

    private final WebClient falWebClient;

    public FalVideoGenerationClient(@Qualifier("falWebClient") WebClient falWebClient) {
        this.falWebClient = falWebClient;
    }

    @Override
    public VideoGenerationSubmitResult submit(VideoGenerationCommand command) {
        FalSubmitResponse response = falWebClient.post()
                .uri(uriBuilder -> {
                    uriBuilder.path(endpointPath(command.modelId()));
                    if (StringUtils.hasText(command.webhookUrl())) {
                        uriBuilder.queryParam("fal_webhook", command.webhookUrl());
                    }
                    return uriBuilder.build();
                })
                .bodyValue(command.input())
                .retrieve()
                .bodyToMono(FalSubmitResponse.class)
                .block();

        return response.toResult();
    }

    @Override
    public VideoGenerationStatusResult getStatus(String modelId, String requestId, boolean withLogs) {
        FalQueueStatusResponse response = falWebClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path(endpointPath(modelId) + "/requests/{requestId}/status");
                    if (withLogs) {
                        uriBuilder.queryParam("logs", "1");
                    }
                    return uriBuilder.build(requestId);
                })
                .retrieve()
                .bodyToMono(FalQueueStatusResponse.class)
                .block();

        return response.toResult();
    }

    @Override
    public VideoGenerationResult getResult(String modelId, String requestId) {
        Map<String, Object> response = falWebClient.get()
                .uri(endpointPath(modelId) + "/requests/{requestId}/response", requestId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .block();

        return new VideoGenerationResult(response);
    }

    private String endpointPath(String modelId) {
        if (!StringUtils.hasText(modelId)) {
            throw new IllegalArgumentException("fal.ai modelId is required.");
        }
        return "/" + modelId.replaceAll("^/+", "").replaceAll("/+$", "");
    }
}
