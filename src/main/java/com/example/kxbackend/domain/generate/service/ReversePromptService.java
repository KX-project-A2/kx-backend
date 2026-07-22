package com.example.kxbackend.domain.generate.service;

import com.example.kxbackend.domain.generate.dto.request.ReversePromptRequestDto;
import com.example.kxbackend.domain.generate.dto.response.ReversePromptResponseDto;
import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.media.entity.enums.MediaType;
import com.example.kxbackend.domain.media.repository.MediaFileRepository;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import com.example.kxbackend.infra.ai.anthropic.ClaudeReversePromptClient;
import com.example.kxbackend.infra.storage.MediaImageDownloadStorageService;
import com.example.kxbackend.infra.storage.MediaImageDownloadStorageService.DownloadedMediaFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

/**
 * 이미지 역프롬프트 추출 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReversePromptService {

    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final ClaudeReversePromptClient claudeReversePromptClient;
    private final MediaFileRepository mediaFileRepository;
    private final MediaImageDownloadStorageService mediaImageDownloadStorageService;

    @Transactional
    public ReversePromptResponseDto extractReversePrompt(
            Long userId,
            ReversePromptRequestDto request,
            MultipartFile imageFile
    ) {
        ImageSource imageSource = resolveImageSource(userId, request.mediaFileId(), imageFile);
        String prompt = claudeReversePromptClient.extract(
                imageSource.content(),
                imageSource.contentType(),
                request.aspectRatio()
        );

        if (imageSource.mediaFile() != null) {
            MediaFile mediaFile = imageSource.mediaFile();
            mediaFile.updateReversePromptResult(prompt, request.aspectRatio());
            mediaFileRepository.save(mediaFile);
        }

        return new ReversePromptResponseDto(
                prompt,
                request.aspectRatio(),
                imageSource.mediaFile() != null ? imageSource.mediaFile().getId() : null
        );
    }

    private ImageSource resolveImageSource(Long userId, Long mediaFileId, MultipartFile imageFile) {
        if (mediaFileId != null) {
            MediaFile mediaFile = mediaFileRepository.findByIdAndUserIdAndDeletedFalse(mediaFileId, userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "미디어 파일을 찾을 수 없습니다."));
            if (mediaFile.getType() != MediaType.IMAGE) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이미지 파일만 역프롬프트 추출에 사용할 수 있습니다.");
            }
            DownloadedMediaFile downloaded = mediaImageDownloadStorageService.download(mediaFile.getFilePath());
            return new ImageSource(downloaded.content(), downloaded.contentType(), mediaFile);
        }

        if (imageFile == null || imageFile.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "역프롬프트 추출할 이미지를 선택하거나 mediaFileId를 지정해야 합니다."
            );
        }
        if (!ALLOWED_IMAGE_CONTENT_TYPES.contains(imageFile.getContentType())) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "이미지는 JPEG, PNG, WEBP, GIF 형식만 지원합니다."
            );
        }
        try {
            return new ImageSource(imageFile.getBytes(), imageFile.getContentType(), null);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "이미지 파일을 읽는 데 실패했습니다.");
        }
    }

    private record ImageSource(byte[] content, String contentType, MediaFile mediaFile) {
    }
}
