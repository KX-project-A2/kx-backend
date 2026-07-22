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

import java.util.List;
import java.util.Map;

/**
 * Claude Sonnet을 이용해 사용자 이미지 프롬프트를 영어로 교정한다.
 */
@Component
@RequiredArgsConstructor
public class ClaudePromptCorrectionClient {

    private static final String SYSTEM_PROMPT = """
            You are an expert prompt editor for AI image generation.
            Rewrite the user's prompt as a clear, detailed, production-ready English image prompt.
            Preserve the user's original intent, subjects, names, constraints, and requested composition.
            Improve visual specificity such as composition, lighting, atmosphere, materials, colors, and camera framing
            only when it does not conflict with the user's request.
            Do not add explanations, headings, quotation marks, markdown, or safety commentary.
            Return only the final English prompt.
            """;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${anthropic.api-key:}")
    private String apiKey;

    @Value("${anthropic.base-url:https://api.anthropic.com/v1}")
    private String baseUrl;

    @Value("${anthropic.prompt-correction.model:claude-sonnet-5}")
    private String model;

    @Value("${anthropic.prompt-correction.max-tokens:1200}")
    private int maxTokens;

    public String correct(String prompt) {
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(
                    ErrorCode.AI_PROVIDER_ERROR,
                    "프롬프트 교정을 위한 Anthropic API 키가 설정되지 않았습니다."
            );
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "system", SYSTEM_PROMPT,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", prompt
                ))
        );

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    baseUrl + "/messages",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    JsonNode.class
            );
            return extractCorrectedPrompt(response.getBody());
        } catch (HttpStatusCodeException exception) {
            throw new BusinessException(
                    ErrorCode.AI_PROVIDER_ERROR,
                    "Claude 프롬프트 교정에 실패했습니다: HTTP " + exception.getStatusCode().value()
                            + " - " + extractApiError(exception.getResponseBodyAsString())
            );
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR, "Claude 프롬프트 교정 요청에 실패했습니다.");
        }
    }

    private String extractCorrectedPrompt(JsonNode responseBody) {
        if (responseBody != null && responseBody.path("content").isArray()) {
            for (JsonNode content : responseBody.path("content")) {
                if ("text".equals(content.path("type").asText())) {
                    String correctedPrompt = content.path("text").asText().trim();
                    if (StringUtils.hasText(correctedPrompt)) {
                        return correctedPrompt;
                    }
                }
            }
        }
        throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR, "Claude 프롬프트 교정 응답이 올바르지 않습니다.");
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
