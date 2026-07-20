package com.example.kxbackend.domain.media.service;

import com.example.kxbackend.domain.media.entity.MediaFavorite;
import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.media.repository.MediaFavoriteRepository;
import com.example.kxbackend.domain.media.repository.MediaFileRepository;
import com.example.kxbackend.domain.user.entity.User;
import com.example.kxbackend.domain.user.repository.UserRepository;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MediaFavoriteService {

    private final MediaFavoriteRepository mediaFavoriteRepository;
    private final MediaFileRepository mediaFileRepository;
    private final UserRepository userRepository;

    @Transactional
    public void addFavorite(Long userId, Long mediaFileId) {
        User user = getUser(userId);
        MediaFile mediaFile = getOwnedMediaFile(userId, mediaFileId);

        if (mediaFavoriteRepository.existsByUserIdAndMediaFileId(userId, mediaFileId)) {
            return;
        }

        try {
            mediaFavoriteRepository.save(MediaFavorite.builder()
                    .user(user)
                    .mediaFile(mediaFile)
                    .build());
        } catch (DataIntegrityViolationException exception) {
            // Concurrent duplicate favorite requests should behave as idempotent success.
        }
    }

    @Transactional
    public void removeFavorite(Long userId, Long mediaFileId) {
        getOwnedMediaFile(userId, mediaFileId);
        mediaFavoriteRepository.deleteByUserIdAndMediaFileId(userId, mediaFileId);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private MediaFile getOwnedMediaFile(Long userId, Long mediaFileId) {
        return mediaFileRepository.findByIdAndUserId(mediaFileId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "미디어 파일을 찾을 수 없습니다."));
    }
}
