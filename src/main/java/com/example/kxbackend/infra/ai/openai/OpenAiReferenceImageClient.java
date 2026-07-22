package com.example.kxbackend.infra.ai.openai;

import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 레퍼런스 이미지의 OpenAI Files 업로드 및 이미지 편집 Batch 제출 클라이언트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiReferenceImageClient {

    private static final String IMAGE_EDITS_ENDPOINT = "/v1/images/edits";

    private final RestTemplate restTemplate;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    /**
     * 레퍼런스 이미지를 OpenAI Files API에 vision 용도로 업로드한다.
     */
    public String uploadReferenceImage(byte[] content, String fileName, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.parseMediaType(contentType));
        ByteArrayResource fileResource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("purpose", "vision");
        body.add("file", new HttpEntity<>(fileResource, fileHeaders));

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/files",
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            Object fileId = response.getBody() != null ? response.getBody().get("id") : null;
            if (fileId == null) {
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "OpenAI 레퍼런스 파일 ID가 없습니다.");
            }
            return String.valueOf(fileId);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR, "OpenAI 레퍼런스 이미지 업로드에 실패했습니다.");
        }
    }

    /**
     * /v1/images/edits JSONL 파일로 이미지 편집 Batch를 제출한다.
     */
    public String submitReferenceBatchJob(String inputFileId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("input_file_id", inputFileId);
        requestBody.put("endpoint", IMAGE_EDITS_ENDPOINT);
        requestBody.put("completion_window", "24h");

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    baseUrl + "/batches",
                    new HttpEntity<>(requestBody, headers),
                    Map.class
            );
            Object batchId = response.getBody() != null ? response.getBody().get("id") : null;
            if (batchId == null) {
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "OpenAI Batch ID가 없습니다.");
            }
            return String.valueOf(batchId);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR, "OpenAI 레퍼런스 이미지 생성 Batch 제출에 실패했습니다.");
        }
    }

    /**
     * Batch 종료 후 OpenAI에 임시 업로드한 레퍼런스 파일을 삭제한다.
     */
    public void deleteReferenceImage(String fileId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        try {
            restTemplate.exchange(
                    baseUrl + "/files/" + fileId,
                    HttpMethod.DELETE,
                    new HttpEntity<>(headers),
                    Void.class
            );
        } catch (RestClientException exception) {
            log.warn("OpenAI 레퍼런스 임시 파일 삭제 실패. fileId={}", fileId, exception);
        }
    }
}
