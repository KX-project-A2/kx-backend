package com.example.kxbackend.domain.share.service;

import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.media.repository.MediaFileRepository;
import com.example.kxbackend.domain.share.dto.response.ShareLinkResponseDto;
import com.example.kxbackend.domain.share.dto.response.SharedMediaResponseDto;
import com.example.kxbackend.domain.share.entity.ShareLink;
import com.example.kxbackend.domain.share.repository.ShareLinkRepository;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShareLinkService {

    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final int DEFAULT_EXPIRATION_DAYS = 7;
    private static final int MAX_TOKEN_GENERATION_ATTEMPTS = 5;

    private final MediaFileRepository mediaFileRepository;
    private final ShareLinkRepository shareLinkRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public ShareLinkResponseDto createShareLink(Long userId, Long mediaFileId) {
        MediaFile mediaFile = mediaFileRepository.findByIdAndUserIdAndDeletedFalse(mediaFileId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "미디어 파일을 찾을 수 없습니다."));

        ShareLink shareLink = ShareLink.builder()
                .mediaFile(mediaFile)
                .token(generateUniqueToken())
                .expiresAt(LocalDateTime.now().plusDays(DEFAULT_EXPIRATION_DAYS))
                .build();

        return ShareLinkResponseDto.from(shareLinkRepository.save(shareLink));
    }

    public SharedMediaResponseDto getSharedMedia(String token) {
        ShareLink shareLink = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "공유 링크를 찾을 수 없습니다."));

        if (!shareLink.isAccessible()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "만료되었거나 사용할 수 없는 공유 링크입니다.");
        }

        return SharedMediaResponseDto.from(shareLink);
    }

    @Transactional
    public void revokeShareLink(Long userId, Long mediaFileId, Long shareLinkId) {
        ShareLink shareLink = shareLinkRepository
                .findByIdAndMediaFileIdAndMediaFileUserId(shareLinkId, mediaFileId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "공유 링크를 찾을 수 없습니다."));

        shareLink.revoke();
    }

    private String generateUniqueToken() {
        for (int attempt = 0; attempt < MAX_TOKEN_GENERATION_ATTEMPTS; attempt++) {
            String token = generateToken();
            if (!shareLinkRepository.existsByToken(token)) {
                return token;
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "공유 토큰 생성에 실패했습니다.");
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
