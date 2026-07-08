package com.example.kxbackend.infra.ai.openai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiBatchClient {

    @Value("${openai.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String BASE_URL = "https://api.openai.com/v1";

    /**
     * 1단계: OpenAI 서버에 JSONL 배치용 파일 업로드
     */
    public String uploadBatchFile(String jsonlContent, String fileName) {
        String url = BASE_URL + "/files";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(apiKey);

        // 메모리 상의 텍스트 데이터를 파일 바이트 리소스로 변환
        ByteArrayResource fileResource = new ByteArrayResource(jsonlContent.getBytes()) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("purpose", "batch");
        body.add("file", fileResource);

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        return (String) response.getBody().get("id"); // file-로 시작하는 고유 ID 리턴
    }

    /**
     * 2단계: 업로드된 파일 ID를 기반으로 비동기 배치 작업 제출
     */
    public String submitBatchJob(String inputFileId) {
        String url = BASE_URL + "/batches";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("input_file_id", inputFileId);
        requestBody.put("endpoint", "/v1/images/generations"); // 이미지 생성 엔드포인트 지정
        requestBody.put("completion_window", "24h");           // OpenAI 배치 표준 24시간 옵션

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        return (String) response.getBody().get("id"); // batch-로 시작하는 고유 ID 리턴
    }

    /**
     * 3단계: 배치 작업의 실시간 진행 현황 확인
     */
    public Map<String, Object> checkBatchStatus(String batchId) {
        String url = BASE_URL + "/batches/" + batchId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        return response.getBody();
    }
}
