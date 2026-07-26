package com.example.kxbackend.infra.ai.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * OpenAI Images API 오류를 한국어 사용자 안내 메시지로 변환한다.
 */
@Component
@RequiredArgsConstructor
public class OpenAiImageErrorMessageResolver {

    private final ObjectMapper objectMapper;

    public String resolve(String responseBody) {
        ParsedError parsed = parse(responseBody);
        return toUserMessage(parsed.code(), parsed.message());
    }

    public String resolveFromExceptionMessage(String exceptionMessage) {
        if (!StringUtils.hasText(exceptionMessage)) {
            return "이미지 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.";
        }

        String lower = exceptionMessage.toLowerCase();
        if (containsAny(lower, "content_policy", "safety", "moderation", "policy")) {
            return "이미지 생성에 실패했습니다. (사유: 콘텐츠 정책 위반) 프롬프트를 수정한 뒤 다시 시도해 주세요.";
        }
        if (containsAny(lower, "rate_limit", "too many requests", "429")) {
            return "이미지 생성 요청이 많아 잠시 후 다시 시도해 주세요.";
        }
        if (containsAny(lower, "insufficient_quota", "billing", "quota")) {
            return "이미지 생성 서비스 사용량 한도에 도달했습니다. 잠시 후 다시 시도해 주세요.";
        }
        if (containsAny(lower, "timeout", "timed out", "connection")) {
            return "이미지 생성 서버와 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.";
        }
        if (containsAny(lower, "invalid", "unsupported")) {
            return "이미지 생성 요청 값이 올바르지 않습니다. 입력값을 확인한 뒤 다시 시도해 주세요.";
        }
        if (exceptionMessage.startsWith("이미지 생성")) {
            return exceptionMessage;
        }
        return "이미지 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.";
    }

    private ParsedError parse(String responseBody) {
        try {
            JsonNode error = objectMapper.readTree(responseBody).path("error");
            String code = error.path("code").asText(null);
            if (!StringUtils.hasText(code)) {
                code = error.path("type").asText(null);
            }
            String message = error.path("message").asText("");
            return new ParsedError(code, message);
        } catch (Exception exception) {
            return new ParsedError(null, "");
        }
    }

    private String toUserMessage(String code, String message) {
        String lowerCode = code == null ? "" : code.toLowerCase();
        String lowerMessage = message == null ? "" : message.toLowerCase();

        if (containsAny(lowerCode, "content_policy")
                || containsAny(lowerMessage, "safety", "moderation", "policy", "content_policy")) {
            return "이미지 생성에 실패했습니다. (사유: 콘텐츠 정책 위반) 프롬프트를 수정한 뒤 다시 시도해 주세요.";
        }
        if (containsAny(lowerCode, "rate_limit")
                || containsAny(lowerMessage, "rate limit", "too many requests")) {
            return "이미지 생성 요청이 많아 잠시 후 다시 시도해 주세요.";
        }
        if (containsAny(lowerCode, "insufficient_quota")
                || containsAny(lowerMessage, "quota", "billing")) {
            return "이미지 생성 서비스 사용량 한도에 도달했습니다. 잠시 후 다시 시도해 주세요.";
        }
        if (containsAny(lowerCode, "invalid_request")
                || containsAny(lowerMessage, "invalid", "unsupported")) {
            return "이미지 생성 요청 값이 올바르지 않습니다. 입력값을 확인한 뒤 다시 시도해 주세요.";
        }
        if (StringUtils.hasText(code)) {
            return "이미지 생성에 실패했습니다. (사유 코드: " + code + ") 잠시 후 다시 시도해 주세요.";
        }
        return "이미지 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.";
    }

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private record ParsedError(String code, String message) {
    }
}
