package com.example.kxbackend.infra.storage;

import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 이미지 업로드 파일 저장소
 */
@Service
public class MediaImageUploadStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif"
    );

    @Value("${storage.local.base-path:./storage}")
    private String basePath;

    /**
     * 업로드 이미지를 저장하고 상대 경로를 반환한다.
     */
    public String upload(Long userId, MultipartFile file) {
        validateImageFile(file);

        try {
            String extension = resolveExtension(file);
            String fileName = UUID.randomUUID() + "." + extension;
            Path directory = Path.of(basePath, "uploads", String.valueOf(userId));
            Files.createDirectories(directory);

            Path filePath = directory.resolve(fileName);
            file.transferTo(filePath.toFile());

            return Path.of("uploads", String.valueOf(userId), fileName).toString();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "이미지 파일 업로드에 실패했습니다.");
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "업로드할 이미지 파일이 없습니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 이미지 형식입니다.");
        }
    }

    private String resolveExtension(MultipartFile file) {
        String contentType = file.getContentType();
        return EXTENSION_BY_CONTENT_TYPE.getOrDefault(contentType, "bin");
    }
}
