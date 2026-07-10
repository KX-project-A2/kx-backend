package com.example.kxbackend.domain.media.service;

import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.media.entity.enums.MediaType;
import com.example.kxbackend.domain.media.repository.MediaFileRepository;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import com.example.kxbackend.infra.storage.MediaImageDownloadStorageService;
import com.example.kxbackend.infra.storage.MediaImageDownloadStorageService.DownloadedMediaFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이미지 다운로드 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaImageDownloadService {

    private final MediaFileRepository mediaFileRepository;
    private final MediaImageDownloadStorageService mediaImageDownloadStorageService;

    /**
     * 업로드된 이미지 파일을 다운로드한다.
     */
    public DownloadedMediaFile downloadImage(Long userId, Long mediaFileId) {
        MediaFile mediaFile = getOwnedMediaFile(userId, mediaFileId);

        if (mediaFile.getType() != MediaType.IMAGE) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "이미지 파일을 찾을 수 없습니다.");
        }

        return mediaImageDownloadStorageService.download(mediaFile.getFilePath());
    }

    private MediaFile getOwnedMediaFile(Long userId, Long mediaFileId) {
        MediaFile mediaFile = mediaFileRepository.findById(mediaFileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "미디어 파일을 찾을 수 없습니다."));

        if (!mediaFile.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "해당 미디어 파일에 접근할 수 없습니다.");
        }

        return mediaFile;
    }
}
