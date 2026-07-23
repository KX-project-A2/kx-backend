package com.example.kxbackend.infra.ai.anthropic;

import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Claude Sonnet Vision으로 이미지에서 이미지 생성용 프롬프트를 추출한다.
 */
@Component
@RequiredArgsConstructor
public class ClaudeReversePromptClient {

    private static final Set<String> SUPPORTED_MEDIA_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private static final String SYSTEM_PROMPT = """
            You are an expert at reverse-engineering images into prompts for AI image generation.
            Analyze the provided image and write one detailed English prompt that could recreate a similar result.
            Include subjects, composition, style, lighting, colors, mood, camera angle, and important details.
            The user will generate a new image with a specific target size/aspect ratio; adapt framing and composition
            hints in the prompt when useful, without inventing unrelated content.
            Do not add explanations, headings, quotation marks, markdown, or safety commentary.
            Return only the final English prompt.
            """;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${anthropic.api-key:}")
    private String apiKey;

    @Value("${anthropic.base-url:https://api.anthropic.com/v1}")
    private String baseUrl;

    @Value("${anthropic.reverse-prompt.model:claude-sonnet-5}")
    private String model;

    @Value("${anthropic.reverse-prompt.max-tokens:1200}")
    private int maxTokens;

    public String extract(byte[] imageBytes, String mediaType, String targetAspectRatio) {
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(
                    ErrorCode.AI_PROVIDER_ERROR,
                    "역프롬프트 추출을 위한 Anthropic API 키가 설정되지 않았습니다."
            );
        }
        String resolvedMediaType = resolveMediaType(mediaType);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> imageBlock = Map.of(
                "type", "image",
                "source", Map.of(
                        "type", "base64",
                        "media_type", resolvedMediaType,
                        "data", base64Image
                )
        );

        Map<String, Object> textBlock = Map.of(
                "type", "text",
                "text", "Target output image aspect ratio for the next generation: " + targetAspectRatio
                        + ". Extract the image generation prompt from this reference image."
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("system", SYSTEM_PROMPT);
        body.put("messages", List.of(Map.of(
                "role", "user",
                "content", List.of(imageBlock, textBlock)
        )));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    baseUrl + "/messages",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    JsonNode.class
            );
            return extractPromptText(response.getBody());
        } catch (HttpStatusCodeException exception) {
            throw new BusinessException(
                    ErrorCode.AI_PROVIDER_ERROR,
                    "Claude 역프롬프트 추출에 실패했습니다: HTTP " + exception.getStatusCode().value()
                            + " - " + extractApiError(exception.getResponseBodyAsString())
            );
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR, "Claude 역프롬프트 추출 요청에 실패했습니다.");
        }
    }

    private String resolveMediaType(String mediaType) {
        if (mediaType != null && SUPPORTED_MEDIA_TYPES.contains(mediaType)) {
            return mediaType;
        }
        return "image/jpeg";
    }

    private String extractPromptText(JsonNode responseBody) {
        if (responseBody != null && responseBody.path("content").isArray()) {
            for (JsonNode content : responseBody.path("content")) {
                if ("text".equals(content.path("type").asText())) {
                    String prompt = content.path("text").asText().trim();
                    if (StringUtils.hasText(prompt)) {
                        return prompt;
                    }
                }
            }
        }
        throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR, "Claude 역프롬프트 추출 응답이 올바르지 않습니다.");
    }

    private String extractApiError(String responseBody) {
        try {
            String message = objectMapper.readTree(responseBody).path("error").path("message").asText();
            return message.isBlank() ? "알 수 없는 오류" : message;
        } catch (Exception exception) {
            return "알 수 없는 오류";
        }
    }
}
