package com.example.kxbackend.domain.generate.service;

import com.example.kxbackend.domain.generate.dto.request.OpenAiGenerateImageRequestDto;
import com.example.kxbackend.domain.generate.dto.request.ReversePromptGenerateRequestDto;
import com.example.kxbackend.domain.generate.dto.request.ReversePromptRequestDto;
import com.example.kxbackend.domain.generate.dto.request.ReversePromptUpdateRequestDto;
import com.example.kxbackend.domain.generate.dto.response.OpenAiGenerateImageJobResponseDto;
import com.example.kxbackend.domain.generate.dto.response.ReversePromptResponseDto;
import com.example.kxbackend.domain.generate.entity.ReversePrompt;
import com.example.kxbackend.domain.generate.repository.ReversePromptRepository;
import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.media.entity.enums.MediaType;
import com.example.kxbackend.domain.media.repository.MediaFileRepository;
import com.example.kxbackend.domain.user.entity.User;
import com.example.kxbackend.domain.user.repository.UserRepository;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import com.example.kxbackend.infra.ai.anthropic.ClaudeReversePromptClient;
import com.example.kxbackend.infra.storage.service.MediaImageDownloadStorageService;
import com.example.kxbackend.infra.storage.service.MediaImageDownloadStorageService.DownloadedMediaFile;
import com.example.kxbackend.infra.storage.validation.UploadedImageFileValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * 이미지 역프롬프트 추출/편집/재생성 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReversePromptService {

    private final ClaudeReversePromptClient claudeReversePromptClient;
    private final ReversePromptRepository reversePromptRepository;
    private final MediaFileRepository mediaFileRepository;
    private final UserRepository userRepository;
    private final MediaImageDownloadStorageService mediaImageDownloadStorageService;
    private final OpenAiGenerateImageService openAiGenerateImageService;

    @Transactional
    public ReversePromptResponseDto extractReversePrompt(
            Long userId,
            ReversePromptRequestDto request,
            MultipartFile imageFile
    ) {
        User user = getUser(userId);
        ImageSource imageSource = resolveImageSource(userId, request.mediaFileId(), imageFile);
        String prompt = claudeReversePromptClient.extract(
                imageSource.content(),
                imageSource.contentType(),
                request.aspectRatio()
        );

        ReversePrompt reversePrompt = reversePromptRepository.save(
                ReversePrompt.builder()
                        .user(user)
                        .sourceMediaFile(imageSource.mediaFile())
                        .prompt(prompt)
                        .aspectRatio(request.aspectRatio())
                        .build()
        );

        if (imageSource.mediaFile() != null) {
            MediaFile mediaFile = imageSource.mediaFile();
            mediaFile.linkReversedPrompt(reversePrompt);
            mediaFileRepository.save(mediaFile);
        }

        return toResponse(reversePrompt);
    }

    public ReversePromptResponseDto getReversePrompt(Long userId, Long reversePromptId) {
        return toResponse(getOwnedReversePrompt(userId, reversePromptId));
    }

    /**
     * 추출된 역프롬프트를 수정한다. (S003)
     */
    @Transactional
    public ReversePromptResponseDto updateReversePrompt(
            Long userId,
            Long reversePromptId,
            ReversePromptUpdateRequestDto request
    ) {
        ReversePrompt reversePrompt = getOwnedReversePrompt(userId, reversePromptId);

        boolean hasPrompt = StringUtils.hasText(request.prompt());
        boolean hasAspectRatio = StringUtils.hasText(request.aspectRatio());
        if (!hasPrompt && !hasAspectRatio) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "수정할 프롬프트 또는 비율을 입력해 주세요.");
        }

        if (hasPrompt) {
            reversePrompt.updatePrompt(request.prompt().trim());
        }
        if (hasAspectRatio) {
            reversePrompt.updateAspectRatio(request.aspectRatio());
        }

        return toResponse(reversePrompt);
    }

    /**
     * 저장된 역프롬프트로 이미지를 재생성한다. (S004)
     */
    @Transactional
    public OpenAiGenerateImageJobResponseDto generateFromReversePrompt(
            Long userId,
            Long reversePromptId,
            ReversePromptGenerateRequestDto request
    ) {
        ReversePrompt reversePrompt = getOwnedReversePrompt(userId, reversePromptId);
        String size = StringUtils.hasText(request.size())
                ? request.size()
                : toImageSize(reversePrompt.getAspectRatio());

        OpenAiGenerateImageRequestDto generateRequest = new OpenAiGenerateImageRequestDto(
                reversePrompt.getPrompt(),
                request.purpose(),
                request.imageCount(),
                size,
                request.quality(),
                request.promptCorrectionEnabled()
        );

        return openAiGenerateImageService.requestImageGeneration(userId, generateRequest, List.of());
    }

    private ReversePrompt getOwnedReversePrompt(Long userId, Long reversePromptId) {
        return reversePromptRepository.findByIdAndUser_Id(reversePromptId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "역프롬프트를 찾을 수 없습니다."));
    }

    private ReversePromptResponseDto toResponse(ReversePrompt reversePrompt) {
        return new ReversePromptResponseDto(
                reversePrompt.getId(),
                reversePrompt.getPrompt(),
                reversePrompt.getAspectRatio(),
                reversePrompt.getSourceMediaFile() == null ? null : reversePrompt.getSourceMediaFile().getId()
        );
    }

    private String toImageSize(String aspectRatio) {
        return switch (aspectRatio) {
            case "1:1" -> "1024x1024";
            case "16:9" -> "1536x1024";
            case "9:16" -> "1024x1536";
            default -> "auto";
        };
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
        UploadedImageFileValidator.validate(imageFile, "역프롬프트 이미지");
        try {
            return new ImageSource(imageFile.getBytes(), imageFile.getContentType(), null);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "이미지 파일을 읽는 데 실패했습니다.");
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private record ImageSource(byte[] content, String contentType, MediaFile mediaFile) {
    }
}
