package com.example.kxbackend.domain.media.service;

import com.example.kxbackend.domain.media.dto.response.MediaFileDownloadUrlResponseDto;
import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.media.repository.MediaFileRepository;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import com.example.kxbackend.infra.storage.ObjectStorageKeys;
import com.example.kxbackend.infra.storage.s3.S3PresignedUrlService;
import com.example.kxbackend.infra.storage.s3.S3PresignedUrlService.PresignedUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaFileDownloadUrlService {

    private final MediaFileRepository mediaFileRepository;
    private final S3PresignedUrlService s3PresignedUrlService;

    public MediaFileDownloadUrlResponseDto createDownloadUrl(Long userId, Long mediaFileId) {
        MediaFile mediaFile = mediaFileRepository.findByIdAndUserIdAndDeletedFalse(mediaFileId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "미디어 파일을 찾을 수 없습니다."));

        String objectKey = resolveS3ObjectKey(mediaFile.getFilePath());
        String fileName = ObjectStorageKeys.fileNameOf(objectKey);
        PresignedUrl presignedUrl = s3PresignedUrlService.createDownloadUrl(objectKey, fileName);

        return new MediaFileDownloadUrlResponseDto(
                presignedUrl.url(),
                presignedUrl.expiresInSeconds()
        );
    }

    private String resolveS3ObjectKey(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "미디어 파일 경로가 없습니다.");
        }
        if (isExternalUrl(filePath)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "S3에 저장된 미디어 파일만 다운로드 URL을 발급할 수 있습니다.");
        }

        String objectKey = ObjectStorageKeys.normalize(filePath);
        if (!StringUtils.hasText(objectKey)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "미디어 파일 경로가 올바르지 않습니다.");
        }
        return objectKey;
    }

    private boolean isExternalUrl(String filePath) {
        return filePath.startsWith("http://") || filePath.startsWith("https://");
    }
}
