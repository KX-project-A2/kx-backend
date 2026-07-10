package com.example.kxbackend.infra.storage;

import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 이미지 다운로드 파일 저장소
 */
@Service
public class MediaImageDownloadStorageService {

    @Value("${storage.local.base-path:./storage}")
    private String basePath;

    /**
     * 다운로드할 이미지 파일을 읽어 반환한다.
     */
    public DownloadedMediaFile download(String relativePath) {
        Path absolutePath = resolveAbsolutePath(relativePath);

        if (!Files.exists(absolutePath) || !Files.isRegularFile(absolutePath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "미디어 파일을 찾을 수 없습니다.");
        }

        try {
            byte[] content = Files.readAllBytes(absolutePath);
            String contentType = Files.probeContentType(absolutePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            return new DownloadedMediaFile(content, contentType, absolutePath.getFileName().toString());
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "미디어 파일 조회에 실패했습니다.");
        }
    }

    private Path resolveAbsolutePath(String relativePath) {
        Path baseDirectory = Path.of(basePath).toAbsolutePath().normalize();
        Path absolutePath = baseDirectory.resolve(relativePath).normalize();

        if (!absolutePath.startsWith(baseDirectory)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "허용되지 않은 파일 경로입니다.");
        }

        return absolutePath;
    }

    public record DownloadedMediaFile(byte[] content, String contentType, String fileName) {
    }
}
