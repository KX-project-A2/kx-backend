package com.example.kxbackend.domain.share.service;

import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.media.repository.MediaFileRepository;
import com.example.kxbackend.domain.share.config.ShareLinkProperties;
import com.example.kxbackend.domain.share.dto.response.ShareLinkResponseDto;
import com.example.kxbackend.domain.share.dto.response.SharedMediaResponseDto;
import com.example.kxbackend.domain.share.entity.ShareLink;
import com.example.kxbackend.domain.share.repository.ShareLinkRepository;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import com.example.kxbackend.infra.storage.ObjectStorageKeys;
import com.example.kxbackend.infra.storage.s3.S3PresignedUrlService;
import com.example.kxbackend.infra.storage.s3.S3PresignedUrlService.PresignedUrl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShareLinkService {

    private final MediaFileRepository mediaFileRepository;
    private final ShareLinkRepository shareLinkRepository;
    private final ShareLinkProperties shareLinkProperties;
    private final S3PresignedUrlService s3PresignedUrlService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public ShareLinkResponseDto createShareLink(Long userId, Long mediaFileId) {
        MediaFile mediaFile = mediaFileRepository.findByIdAndUserIdAndDeletedFalse(mediaFileId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "미디어 파일을 찾을 수 없습니다."));

        ShareLink shareLink = ShareLink.builder()
                .mediaFile(mediaFile)
                .token(generateUniqueToken())
                .expiresAt(LocalDateTime.now().plusDays(shareLinkProperties.getExpirationDays()))
                .build();

        return ShareLinkResponseDto.from(shareLinkRepository.save(shareLink));
    }

    public SharedMediaResponseDto getSharedMedia(String token) {
        ShareLink shareLink = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "공유 링크를 찾을 수 없습니다."));

        if (!shareLink.isAccessible()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "만료되었거나 사용할 수 없는 공유 링크입니다.");
        }

        return createSharedMediaResponse(shareLink);
    }

    @Transactional
    public void revokeShareLink(Long userId, Long mediaFileId, Long shareLinkId) {
        ShareLink shareLink = shareLinkRepository
                .findByIdAndMediaFileIdAndMediaFileUserId(shareLinkId, mediaFileId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "공유 링크를 찾을 수 없습니다."));

        shareLink.revoke();
    }

    private String generateUniqueToken() {
        for (int attempt = 0; attempt < shareLinkProperties.getMaxTokenGenerationAttempts(); attempt++) {
            String token = generateToken();
            if (!shareLinkRepository.existsByToken(token)) {
                return token;
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "공유 토큰 생성에 실패했습니다.");
    }

    private String generateToken() {
        byte[] bytes = new byte[shareLinkProperties.getTokenByteLength()];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private SharedMediaResponseDto createSharedMediaResponse(ShareLink shareLink) {
        MediaFile mediaFile = shareLink.getMediaFile();
        String filePath = mediaFile.getFilePath();
        if (!StringUtils.hasText(filePath)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "미디어 파일 경로가 비어 있습니다.");
        }
        if (isExternalUrl(filePath)) {
            return SharedMediaResponseDto.from(shareLink, filePath, filePath, null);
        }

        String objectKey = ObjectStorageKeys.normalize(filePath);
        PresignedUrl readUrl = s3PresignedUrlService.createReadUrl(objectKey);
        PresignedUrl downloadUrl = s3PresignedUrlService.createDownloadUrl(
                objectKey,
                ObjectStorageKeys.fileNameOf(objectKey)
        );

        return SharedMediaResponseDto.from(
                shareLink,
                readUrl.url(),
                downloadUrl.url(),
                downloadUrl.expiresInSeconds()
        );
    }

    private boolean isExternalUrl(String filePath) {
        return StringUtils.hasText(filePath)
                && (filePath.startsWith("http://") || filePath.startsWith("https://"));
    }
}
