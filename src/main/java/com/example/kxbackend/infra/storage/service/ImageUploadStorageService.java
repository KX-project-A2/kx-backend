package com.example.kxbackend.infra.storage.service;

import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import com.example.kxbackend.infra.storage.ObjectStorage;
import com.example.kxbackend.infra.storage.ObjectStorageKeys;
import com.example.kxbackend.infra.storage.validation.UploadedImageFileValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

/**
 * 이미지 업로드 파일 저장소
 */
@Service
@RequiredArgsConstructor
public class ImageUploadStorageService {

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final ObjectStorage objectStorage;

    /**
     * 사용자 업로드 이미지를 저장하고 object key를 반환한다.
     */
    public String uploadMediaImage(Long userId, MultipartFile file) {
        return uploadImage(userId, file, ImageObjectKeyType.MEDIA);
    }

    /**
     * 프로필 이미지를 저장하고 object key를 반환한다.
     */
    public String uploadProfileImage(Long userId, MultipartFile file) {
        return uploadImage(userId, file, ImageObjectKeyType.PROFILE);
    }

    public void delete(String objectKey) {
        objectStorage.delete(objectKey);
    }

    private String uploadImage(Long userId, MultipartFile file, ImageObjectKeyType objectKeyType) {
        UploadedImageFileValidator.validate(file, "이미지");

        try {
            String extension = resolveExtension(file);
            String fileName = UUID.randomUUID() + "." + extension;
            String objectKey = objectKeyType.createObjectKey(userId, fileName);

            objectStorage.put(objectKey, file.getBytes(), file.getContentType());
            return objectKey;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "이미지 파일 업로드에 실패했습니다.");
        }
    }

    private String resolveExtension(MultipartFile file) {
        String contentType = file.getContentType();
        return EXTENSION_BY_CONTENT_TYPE.getOrDefault(contentType, "bin");
    }

    private enum ImageObjectKeyType {
        MEDIA {
            @Override
            String createObjectKey(Long userId, String fileName) {
                return ObjectStorageKeys.uploadImageKey(userId, fileName);
            }
        },
        PROFILE {
            @Override
            String createObjectKey(Long userId, String fileName) {
                return ObjectStorageKeys.profileImageKey(userId, fileName);
            }
        };

        abstract String createObjectKey(Long userId, String fileName);
    }
}
