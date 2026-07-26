package com.example.kxbackend.infra.ai.openai;

import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Images API 동기 생성/편집 클라이언트
 */
@Component
@RequiredArgsConstructor
public class OpenAiGenerateImageClient {

    private final RestTemplate restTemplate;
    private final OpenAiImageErrorMessageResolver openAiImageErrorMessageResolver;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    public List<byte[]> generate(
            String model,
            String prompt,
            int imageCount,
            String size,
            String quality
    ) {
        Map<String, Object> body = createCommonBody(model, prompt, imageCount, size, quality);
        return requestImages("/images/generations", body);
    }

    public List<byte[]> edit(
            String model,
            String prompt,
            int imageCount,
            String size,
            String quality,
            List<String> referenceFileIds
    ) {
        Map<String, Object> body = createCommonBody(model, prompt, imageCount, size, quality);
        body.put("images", referenceFileIds.stream()
                .map(fileId -> Map.of("file_id", fileId))
                .toList());
        return requestImages("/images/edits", body);
    }

    private Map<String, Object> createCommonBody(
            String model,
            String prompt,
            int imageCount,
            String size,
            String quality
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("prompt", prompt);
        body.put("n", imageCount);
        body.put("size", size);
        body.put("quality", quality);
        return body;
    }

    private List<byte[]> requestImages(String endpoint, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    baseUrl + endpoint,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    JsonNode.class
            );
            return extractImages(response.getBody());
        } catch (HttpStatusCodeException exception) {
            throw new BusinessException(
                    ErrorCode.AI_PROVIDER_ERROR,
                    openAiImageErrorMessageResolver.resolve(exception.getResponseBodyAsString())
            );
        } catch (RestClientException exception) {
            throw new BusinessException(
                    ErrorCode.AI_PROVIDER_ERROR,
                    "이미지 생성 서버와 연결하지 못했습니다. 잠시 후 다시 시도해 주세요."
            );
        }
    }

    private List<byte[]> extractImages(JsonNode responseBody) {
        if (responseBody == null || !responseBody.path("data").isArray()) {
            throw new BusinessException(
                    ErrorCode.AI_PROVIDER_ERROR,
                    "이미지 생성 응답이 올바르지 않습니다. 잠시 후 다시 시도해 주세요."
            );
        }

        List<byte[]> images = new ArrayList<>();
        for (JsonNode imageNode : responseBody.path("data")) {
            String base64 = imageNode.path("b64_json").asText(null);
            if (base64 != null && !base64.isBlank()) {
                images.add(Base64.getDecoder().decode(base64));
                continue;
            }

            String imageUrl = imageNode.path("url").asText(null);
            if (imageUrl != null && !imageUrl.isBlank()) {
                byte[] content = restTemplate.getForObject(imageUrl, byte[].class);
                if (content != null && content.length > 0) {
                    images.add(content);
                }
            }
        }

        if (images.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.AI_PROVIDER_ERROR,
                    "생성된 이미지를 받지 못했습니다. 잠시 후 다시 시도해 주세요."
            );
        }
        return List.copyOf(images);
    }
}
