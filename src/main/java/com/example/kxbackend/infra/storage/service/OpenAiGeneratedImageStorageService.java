package com.example.kxbackend.infra.storage.service;

import com.example.kxbackend.infra.storage.ObjectStorage;
import com.example.kxbackend.infra.storage.ObjectStorageKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * OpenAI로 생성된 이미지를 저장소에 저장한다.
 */
@Service
@RequiredArgsConstructor
public class OpenAiGeneratedImageStorageService {

    private static final String CONTENT_TYPE = "image/png";

    private final ObjectStorage objectStorage;

    /**
     * 생성된 이미지 바이너리를 저장하고 object key를 반환한다.
     * 예: openai-images/{userId}/{uuid}.png
     */
    public String saveGeneratedImage(Long userId, byte[] imageBytes) {
        String fileName = UUID.randomUUID() + ".png";
        String objectKey = ObjectStorageKeys.generatedImageKey(userId, fileName);
        objectStorage.put(objectKey, imageBytes, CONTENT_TYPE);
        return objectKey;
    }
}
