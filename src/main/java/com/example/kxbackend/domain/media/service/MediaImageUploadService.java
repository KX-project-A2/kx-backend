package com.example.kxbackend.domain.media.service;

import com.example.kxbackend.domain.media.dto.response.MediaImageUploadResponseDto;
import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.media.entity.enums.MediaType;
import com.example.kxbackend.domain.media.repository.MediaFileRepository;
import com.example.kxbackend.domain.user.entity.User;
import com.example.kxbackend.domain.user.repository.UserRepository;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import com.example.kxbackend.infra.storage.service.MediaImageUploadStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 업로드 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaImageUploadService {

    private final MediaFileRepository mediaFileRepository;
    private final UserRepository userRepository;
    private final MediaImageUploadStorageService mediaImageUploadStorageService;

    /**
     * 이미지 파일을 업로드한다.
     */
    @Transactional
    public MediaImageUploadResponseDto uploadImage(Long userId, MultipartFile file, String tags) {
        User user = getUser(userId);
        String savedPath = mediaImageUploadStorageService.upload(userId, file);

        MediaFile mediaFile = MediaFile.builder()
                .user(user)
                .type(MediaType.IMAGE)
                .filePath(savedPath)
                .tags(tags)
                .build();

        MediaFile savedMediaFile = mediaFileRepository.save(mediaFile);
        return MediaImageUploadResponseDto.from(savedMediaFile);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }
}
