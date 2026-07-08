package com.example.kxbackend.infra.storage;

import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * OpenAI로 생성된 이미지를 로컬 디스크에 저장한다.
 */
@Service
public class OpenAiGeneratedImageStorageService {

    @Value("${storage.local.base-path:./storage}")
    private String basePath;

    /**
     * 생성된 이미지 바이너리를 저장하고 상대 경로를 반환한다.
     */
    public String saveGeneratedImage(Long userId, byte[] imageBytes) {
        try {
            String fileName = UUID.randomUUID() + ".png";
            Path directory = Path.of(basePath, "openai-images", String.valueOf(userId));
            Files.createDirectories(directory);

            Path filePath = directory.resolve(fileName);
            Files.write(filePath, imageBytes);

            return Path.of("openai-images", String.valueOf(userId), fileName).toString();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "생성 이미지 파일 저장에 실패했습니다.");
        }
    }
}
