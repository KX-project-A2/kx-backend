package com.example.kxbackend.infra.storage.local;

import com.example.kxbackend.infra.storage.ObjectStorage;
import com.example.kxbackend.infra.storage.ObjectStorageKeys;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 로컬 디스크 기반 객체 저장소
 */
public class LocalObjectStorage implements ObjectStorage {

    private final Path baseDirectory;

    public LocalObjectStorage(String basePath) {
        this.baseDirectory = Path.of(basePath).toAbsolutePath().normalize();
    }

    @Override
    public void put(String objectKey, byte[] content, String contentType) {
        Path absolutePath = resolveAbsolutePath(objectKey);
        try {
            Files.createDirectories(absolutePath.getParent());
            Files.write(absolutePath, content);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "파일 저장에 실패했습니다.");
        }
    }

    @Override
    public StoredObject get(String objectKey) {
        Path absolutePath = resolveAbsolutePath(objectKey);
        if (!Files.exists(absolutePath) || !Files.isRegularFile(absolutePath)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "미디어 파일을 찾을 수 없습니다.");
        }

        try {
            byte[] content = Files.readAllBytes(absolutePath);
            String contentType = Files.probeContentType(absolutePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            return new StoredObject(content, contentType, absolutePath.getFileName().toString());
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "미디어 파일 조회에 실패했습니다.");
        }
    }

    @Override
    public boolean exists(String objectKey) {
        Path absolutePath = resolveAbsolutePath(objectKey);
        return Files.exists(absolutePath) && Files.isRegularFile(absolutePath);
    }

    private Path resolveAbsolutePath(String objectKey) {
        String normalizedKey = ObjectStorageKeys.normalize(objectKey);
        Path absolutePath = baseDirectory.resolve(normalizedKey).normalize();

        if (!absolutePath.startsWith(baseDirectory)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "허용되지 않은 파일 경로입니다.");
        }
        return absolutePath;
    }
}
