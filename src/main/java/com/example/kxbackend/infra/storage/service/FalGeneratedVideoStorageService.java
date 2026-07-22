package com.example.kxbackend.infra.storage.service;

import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import com.example.kxbackend.infra.storage.ObjectStorage;
import com.example.kxbackend.infra.storage.ObjectStorageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * fal.ai 생성 영상 결과를 저장소에 저장한다.
 */
@Service
@RequiredArgsConstructor
public class FalGeneratedVideoStorageService {

    private static final String DEFAULT_CONTENT_TYPE = "video/mp4";
    private static final String FILE_EXTENSION = ".mp4";

    private final ObjectStorage objectStorage;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public String saveGeneratedVideo(Long userId, String videoUrl) {
        validateInput(userId, videoUrl);

        Path tempFile = null;
        try {
            // fal 영상 응답을 먼저 받을 임시 파일 생성
            tempFile = Files.createTempFile("fal-video-", ".tmp");
            // fal 영상 URL을 임시 파일로 다운로드
            HttpResponse<Path> response = downloadToTempFile(videoUrl, tempFile);
            validateDownloadResponse(response);

            // S3 stream 업로드에 사용할 실제 파일 크기 확인
            long contentLength = Files.size(tempFile);
            if (contentLength <= 0) {
                throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR, "fal.ai 영상 파일이 비어 있습니다.");
            }

            // 응답 content-type이 없으면 video/mp4 기본값 사용
            String contentType = resolveContentType(response);
            // DB에 저장할 영상 object key 생성
            String objectKey = ObjectStorageKeys.generatedVideoKey(userId, UUID.randomUUID() + FILE_EXTENSION);
            // 임시 파일을 stream으로 열어 ObjectStorage에 저장
            try (InputStream inputStream = Files.newInputStream(tempFile)) {
                objectStorage.put(objectKey, inputStream, contentLength, contentType);
            }
            return objectKey;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "영상 파일 저장에 실패했습니다.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "영상 파일 다운로드가 중단되었습니다.");
        } finally {
            // 저장 성공/실패와 관계없이 임시 파일 삭제
            deleteTempFile(tempFile);
        }
    }

    private void validateInput(Long userId, String videoUrl) {
        // 저장 대상 사용자와 fal 영상 URL 필수값 검증
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (!StringUtils.hasText(videoUrl)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "저장할 영상 URL이 필요합니다.");
        }
    }

    private HttpResponse<Path> downloadToTempFile(String videoUrl, Path tempFile) throws IOException, InterruptedException {
        // redirect를 따라가며 fal 영상 파일 다운로드
        HttpRequest request = HttpRequest.newBuilder(URI.create(videoUrl))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofFile(tempFile));
    }

    private void validateDownloadResponse(HttpResponse<Path> response) {
        // fal 파일 다운로드 HTTP 상태 검증
        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR, "fal.ai 영상 파일 다운로드에 실패했습니다. HTTP " + statusCode);
        }
    }

    private String resolveContentType(HttpResponse<Path> response) {
        // fal 응답 헤더의 content-type 추출
        return response.headers()
                .firstValue("content-type")
                .filter(StringUtils::hasText)
                .orElse(DEFAULT_CONTENT_TYPE);
    }

    private void deleteTempFile(Path tempFile) {
        // 임시 파일이 없으면 삭제 생략
        if (tempFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException ignored) {
        }
    }
}
