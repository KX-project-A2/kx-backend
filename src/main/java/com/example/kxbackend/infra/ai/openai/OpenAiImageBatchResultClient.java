package com.example.kxbackend.infra.ai.openai;

import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 배치 이미지 생성 결과 처리 클라이언트
 * - OpenAiBatchClient가 담당하지 않는 결과 다운로드/파싱을 수행한다.
 */
@Component
@RequiredArgsConstructor
public class OpenAiImageBatchResultClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    /**
     * OpenAiBatchClient의 배치 상태 응답을 타입으로 변환한다.
     */
    public OpenAiBatchStatusResponse parseBatchStatus(Map<String, Object> statusMap) {
        if (statusMap == null || !statusMap.containsKey("status")) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "배치 상태 조회 응답이 올바르지 않습니다.");
        }

        String status = String.valueOf(statusMap.get("status"));
        String outputFileId = statusMap.get("output_file_id") != null
                ? String.valueOf(statusMap.get("output_file_id"))
                : null;
        String errorFileId = statusMap.get("error_file_id") != null
                ? String.valueOf(statusMap.get("error_file_id"))
                : null;

        return new OpenAiBatchStatusResponse(status, outputFileId, errorFileId);
    }

    /**
     * 배치 에러 파일(JSONL)에서 첫 번째 오류 메시지를 추출한다.
     */
    public String extractErrorMessage(String errorFileId) {
        try {
            String jsonlContent = downloadFileContent(errorFileId);
            String firstLine = jsonlContent.lines()
                    .filter(line -> !line.isBlank())
                    .findFirst()
                    .orElse(null);
            if (firstLine == null) {
                return null;
            }

            JsonNode root = objectMapper.readTree(firstLine);
            JsonNode messageNode = root.path("error").path("message");
            if (messageNode.isTextual() && !messageNode.asText().isBlank()) {
                return messageNode.asText();
            }
            messageNode = root.path("response").path("body").path("error").path("message");
            return messageNode.isTextual() && !messageNode.asText().isBlank() ? messageNode.asText() : null;
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * 배치 결과 JSONL 파일을 다운로드한다.
     */
    public String downloadFileContent(String fileId) {
        String url = baseUrl + "/files/" + fileId + "/content";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getBody() == null || response.getBody().isBlank()) {
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "결과 파일이 비어 있습니다.");
            }
            return response.getBody();
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "결과 파일 다운로드에 실패했습니다.");
        }
    }

    /**
     * JSONL 결과에서 생성된 이미지 바이너리 목록을 추출한다.
     */
    public List<byte[]> extractAllImageBytes(String jsonlContent) {
        try {
            String firstLine = jsonlContent.lines()
                    .filter(line -> !line.isBlank())
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "결과 JSONL이 비어 있습니다."));

            JsonNode root = objectMapper.readTree(firstLine);
            JsonNode errorNode = root.path("error");
            if (!errorNode.isMissingNode() && !errorNode.isNull()) {
                String errorMessage = errorNode.path("message").asText("OpenAI 이미지 생성 API 오류");
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, errorMessage);
            }

            JsonNode dataNode = root.path("response").path("body").path("data");
            if (!dataNode.isArray() || dataNode.isEmpty()) {
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "생성된 이미지 데이터를 찾을 수 없습니다.");
            }

            List<byte[]> imageBytesList = new ArrayList<>();
            for (JsonNode imageNode : dataNode) {
                imageBytesList.add(extractImageBytesFromNode(imageNode));
            }

            return imageBytesList;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "배치 결과 파싱에 실패했습니다.");
        }
    }

    /**
     * JSONL 결과에서 첫 번째 이미지 바이너리를 추출한다.
     */
    public byte[] extractImageBytes(String jsonlContent) {
        return extractAllImageBytes(jsonlContent).getFirst();
    }

    private byte[] extractImageBytesFromNode(JsonNode dataNode) {
        JsonNode imageUrlNode = dataNode.path("url");
        if (imageUrlNode.isTextual() && !imageUrlNode.asText().isBlank()) {
            return downloadImageFromUrl(imageUrlNode.asText());
        }

        JsonNode base64Node = dataNode.path("b64_json");
        if (base64Node.isTextual() && !base64Node.asText().isBlank()) {
            return Base64.getDecoder().decode(base64Node.asText());
        }

        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "이미지 URL 또는 base64 데이터를 찾을 수 없습니다.");
    }

    private byte[] downloadImageFromUrl(String imageUrl) {
        try {
            ResponseEntity<byte[]> response = restTemplate.getForEntity(imageUrl, byte[].class);
            if (response.getBody() == null || response.getBody().length == 0) {
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "이미지 다운로드 결과가 비어 있습니다.");
            }
            return response.getBody();
        } catch (RestClientException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "생성된 이미지 다운로드에 실패했습니다.");
        }
    }

    public record OpenAiBatchStatusResponse(String status, String outputFileId, String errorFileId) {
    }
}
