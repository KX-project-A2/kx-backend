package com.example.kxbackend.infra.storage.service;

import com.example.kxbackend.infra.storage.ObjectStorage;
import com.example.kxbackend.infra.storage.ObjectStorage.StoredObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 이미지 다운로드 파일 저장소
 */
@Service
@RequiredArgsConstructor
public class MediaImageDownloadStorageService {

    private final ObjectStorage objectStorage;

    /**
     * object key로 이미지 파일을 읽어 반환한다.
     */
    public DownloadedMediaFile download(String objectKey) {
        StoredObject storedObject = objectStorage.get(objectKey);
        return new DownloadedMediaFile(
                storedObject.content(),
                storedObject.contentType(),
                storedObject.fileName()
        );
    }

    public record DownloadedMediaFile(byte[] content, String contentType, String fileName) {
    }
}
