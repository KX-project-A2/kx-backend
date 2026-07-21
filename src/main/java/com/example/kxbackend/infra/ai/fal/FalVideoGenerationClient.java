package com.example.kxbackend.infra.ai.fal;

import com.example.kxbackend.domain.generate.client.VideoGenerationClient;
import com.example.kxbackend.domain.generate.client.VideoGenerationClientException;
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
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.URI;
import java.util.Map;

@Component
public class FalVideoGenerationClient implements VideoGenerationClient {

    private final WebClient falWebClient;

    public FalVideoGenerationClient(@Qualifier("falWebClient") WebClient falWebClient) {
        this.falWebClient = falWebClient;
    }

    @Override
    public VideoGenerationSubmitResult submit(VideoGenerationCommand command) {
        try {
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
        } catch (WebClientResponseException exception) {
            throw toClientException(exception);
        }
    }

    @Override
    public VideoGenerationStatusResult getStatus(String modelId, String requestId, boolean withLogs) {
        try {
            FalQueueStatusResponse response = falWebClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path(queueEndpointPath(modelId) + "/requests/{requestId}/status");
                        if (withLogs) {
                            uriBuilder.queryParam("logs", "1");
                        }
                        return uriBuilder.build(requestId);
                    })
                    .retrieve()
                    .bodyToMono(FalQueueStatusResponse.class)
                    .block();

            return response.toResult();
        } catch (WebClientResponseException exception) {
            throw toClientException(exception);
        }
    }

    @Override
    public VideoGenerationStatusResult getStatusByUrl(String statusUrl, boolean withLogs) {
        try {
            FalQueueStatusResponse response = falWebClient.get()
                    .uri(URI.create(withLogs ? appendQueryParam(statusUrl, "logs=1") : statusUrl))
                    .retrieve()
                    .bodyToMono(FalQueueStatusResponse.class)
                    .block();

            return response.toResult();
        } catch (WebClientResponseException exception) {
            throw toClientException(exception);
        }
    }

    @Override
    public VideoGenerationResult getResult(String modelId, String requestId) {
        try {
            Map<String, Object> response = falWebClient.get()
                    .uri(queueEndpointPath(modelId) + "/requests/{requestId}", requestId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();

            return new VideoGenerationResult(response);
        } catch (WebClientResponseException exception) {
            throw toClientException(exception);
        }
    }

    @Override
    public VideoGenerationResult getResultByUrl(String responseUrl) {
        try {
            Map<String, Object> response = falWebClient.get()
                    .uri(URI.create(responseUrl))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();

            return new VideoGenerationResult(response);
        } catch (WebClientResponseException exception) {
            throw toClientException(exception);
        }
    }

    private String endpointPath(String modelId) {
        if (!StringUtils.hasText(modelId)) {
            throw new IllegalArgumentException("fal.ai modelId is required.");
        }
        return "/" + modelId.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private String queueEndpointPath(String modelId) {
        String normalizedModelId = modelId.replaceAll("^/+", "").replaceAll("/+$", "");
        return "/" + normalizedModelId;
    }

    private String appendQueryParam(String url, String queryParam) {
        return url.contains("?") ? url + "&" + queryParam : url + "?" + queryParam;
    }

    private VideoGenerationClientException toClientException(WebClientResponseException exception) {
        return new VideoGenerationClientException(
                exception.getStatusCode().value(),
                exception.getResponseBodyAsString()
        );
    }
}
