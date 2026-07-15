package com.example.kxbackend.domain.media.service;

import com.example.kxbackend.domain.media.dto.response.MediaFilePageResponseDto;
import com.example.kxbackend.domain.media.dto.response.MediaFileResponseDto;
import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.media.entity.enums.MediaType;
import com.example.kxbackend.domain.media.repository.MediaFileRepository;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaFileQueryService {

    private final MediaFileRepository mediaFileRepository;

    public MediaFilePageResponseDto getMediaFiles(Long userId, MediaType type, Pageable pageable) {
        Page<MediaFile> mediaFiles = type == null
                ? mediaFileRepository.findAllByUserId(userId, pageable)
                : mediaFileRepository.findAllByUserIdAndType(userId, type, pageable);

        return MediaFilePageResponseDto.from(mediaFiles.map(MediaFileResponseDto::from));
    }

    public MediaFileResponseDto getMediaFile(Long userId, Long mediaFileId) {
        MediaFile mediaFile = mediaFileRepository.findByIdAndUserId(mediaFileId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "미디어 파일을 찾을 수 없습니다."));

        return MediaFileResponseDto.from(mediaFile);
    }
}
