package com.example.kxbackend.domain.generate.service;

import com.example.kxbackend.domain.generate.client.VideoGenerationClient;
import com.example.kxbackend.domain.generate.client.dto.VideoGenerationCommand;
import com.example.kxbackend.domain.generate.client.dto.VideoGenerationResult;
import com.example.kxbackend.domain.generate.client.dto.VideoGenerationStatusResult;
import com.example.kxbackend.domain.generate.client.dto.VideoGenerationSubmitResult;
import com.example.kxbackend.domain.generate.dto.request.FalWebhookRequestDto;
import com.example.kxbackend.domain.generate.dto.request.ImageToVideoGenerateRequestDto;
import com.example.kxbackend.domain.generate.dto.response.GenerateJobStatusResponseDto;
import com.example.kxbackend.domain.generate.entity.GenerateJob;
import com.example.kxbackend.domain.generate.entity.GeneratePrompt;
import com.example.kxbackend.domain.generate.entity.enums.PromptKind;
import com.example.kxbackend.domain.generate.entity.enums.Status;
import com.example.kxbackend.domain.generate.entity.enums.Type;
import com.example.kxbackend.domain.generate.repository.GenerateJobRepository;
import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.media.entity.enums.MediaType;
import com.example.kxbackend.domain.media.repository.MediaFileRepository;
import com.example.kxbackend.domain.user.entity.User;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GenerateService {

    private static final String KLING_O3_STANDARD_IMAGE_TO_VIDEO_MODEL_ID =
            "fal-ai/kling-video/o3/standard/image-to-video";

    private final GenerateJobRepository generateJobRepository;
    private final MediaFileRepository mediaFileRepository;
    private final VideoGenerationClient videoGenerationClient;

    /**
     * 요청 DTO 기반 이미지-영상 생성 작업 생성
     */
    @Transactional
    public GenerateJob createImageToVideoJob(User user, ImageToVideoGenerateRequestDto request) {
        return createImageToVideoJob(
                user,
                request.inputMediaFileId(),
                request.modelId(),
                request.prompt(),
                request.webhookUrl(),
                request.options()
        );
    }

    /**
     * fal.ai webhook 결과를 작업 상태와 결과 영상으로 반영
     */
    @Transactional
    public GenerateJob handleFalWebhook(FalWebhookRequestDto request) {
        if (request == null || !StringUtils.hasText(request.requestId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "fal.ai request_id가 필요합니다.");
        }

        GenerateJob generateJob = generateJobRepository.findByFalRequestId(request.requestId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "생성 작업을 찾을 수 없습니다."));

        if (generateJob.getStatus() == Status.COMPLETED || generateJob.getStatus() == Status.FAILED) {
            return generateJob;
        }

        if (isFailureStatus(request.status()) || StringUtils.hasText(request.error())) {
            generateJob.fail(StringUtils.hasText(request.error()) ? request.error() : request.status());
            return generateJob;
        }

        String videoUrl = extractVideoUrl(request.payload());
        if (!StringUtils.hasText(videoUrl)) {
            generateJob.fail("fal.ai webhook payload에서 영상 URL을 찾을 수 없습니다.");
            return generateJob;
        }

        completeVideoJob(generateJob, videoUrl);
        return generateJob;
    }

    /**
     * 영상 생성 작업 상태 조회
     */
    @Transactional
    public GenerateJobStatusResponseDto getImageToVideoJobStatus(User user, Long jobId) {
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (jobId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "생성 작업 ID가 필요합니다.");
        }

        GenerateJob generateJob = generateJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "영상 생성 작업을 찾을 수 없습니다."));

        if (!generateJob.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "해당 영상 생성 작업에 접근할 수 없습니다.");
        }

        if (generateJob.getType() != Type.IMAGE_TO_VIDEO) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "영상 생성 작업을 찾을 수 없습니다.");
        }

        VideoGenerationStatusResult falStatus = getFalStatus(generateJob);
        completeByPollingResultIfPossible(generateJob, falStatus);
        return GenerateJobStatusResponseDto.from(generateJob, falStatus);
    }

    /**
     * 입력 이미지 URL과 프롬프트를 fal.ai 생성 요청으로 제출
     */
    @Transactional
    public GenerateJob createImageToVideoJob(
            User user,
            Long inputMediaFileId,
            String modelId,
            String prompt,
            String webhookUrl,
            Map<String, Object> options
    ) {
        String resolvedModelId = resolveFalModelId(modelId);
        validateImageToVideoInput(user, inputMediaFileId, prompt);

        MediaFile inputMediaFile = mediaFileRepository.findByIdAndUserId(inputMediaFileId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "입력 이미지 파일을 찾을 수 없습니다."));

        if (!StringUtils.hasText(inputMediaFile.getFilePath())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "입력 이미지 파일 경로가 필요합니다.");
        }

        GenerateJob generateJob = GenerateJob.builder()
                .user(user)
                .type(Type.IMAGE_TO_VIDEO)
                .status(Status.CREATED)
                .inputMediaFile(inputMediaFile)
                .build();
        generateJob.addPrompt(PromptKind.SCENE, 1, prompt);

        GenerateJob savedJob = generateJobRepository.save(generateJob);

        VideoGenerationSubmitResult submitResult = videoGenerationClient.submit(
                new VideoGenerationCommand(
                        resolvedModelId,
                        buildImageToVideoInput(inputMediaFile.getFilePath(), prompt, options),
                        webhookUrl
                )
        );

        savedJob.submit(
                submitResult.requestId(),
                resolvedModelId,
                submitResult.statusUrl(),
                submitResult.responseUrl()
        );
        return savedJob;
    }

    /**
     * Helper 메서드
     */
    private Map<String, Object> buildImageToVideoInput(
            String imageUrl,
            String prompt,
            Map<String, Object> options
    ) {
        Map<String, Object> input = new LinkedHashMap<>();
        if (options != null) {
            input.putAll(options);
        }
        input.put("image_url", imageUrl);
        input.put("prompt", prompt);
        return input;
    }

    private void validateImageToVideoInput(
            User user,
            Long inputMediaFileId,
            String prompt
    ) {
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (inputMediaFileId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "입력 이미지 파일 ID가 필요합니다.");
        }
        if (!StringUtils.hasText(prompt)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "프롬프트가 필요합니다.");
        }
    }

    private String resolveFalModelId(String modelId) {
        if (StringUtils.hasText(modelId)) {
            return modelId;
        }
        return KLING_O3_STANDARD_IMAGE_TO_VIDEO_MODEL_ID;
    }

    private boolean isFailureStatus(String status) {
        return "FAILED".equalsIgnoreCase(status)
                || "ERROR".equalsIgnoreCase(status);
    }

    private VideoGenerationStatusResult getFalStatus(GenerateJob generateJob) {
        if (!StringUtils.hasText(generateJob.getFalModelId()) || !StringUtils.hasText(generateJob.getFalRequestId())) {
            return null;
        }
        if (generateJob.getStatus() == Status.COMPLETED
                || generateJob.getStatus() == Status.FAILED
                || generateJob.getStatus() == Status.CANCELED) {
            return null;
        }
        if (StringUtils.hasText(generateJob.getFalStatusUrl())) {
            return videoGenerationClient.getStatusByUrl(generateJob.getFalStatusUrl(), true);
        }
        return videoGenerationClient.getStatus(generateJob.getFalModelId(), generateJob.getFalRequestId(), true);
    }

    private void completeByPollingResultIfPossible(GenerateJob generateJob, VideoGenerationStatusResult falStatus) {
        if (generateJob.getStatus() == Status.COMPLETED
                || falStatus == null
                || !"COMPLETED".equalsIgnoreCase(falStatus.status())) {
            return;
        }

        VideoGenerationResult result = StringUtils.hasText(generateJob.getFalResponseUrl())
                ? videoGenerationClient.getResultByUrl(generateJob.getFalResponseUrl())
                : videoGenerationClient.getResult(generateJob.getFalModelId(), generateJob.getFalRequestId());

        String videoUrl = extractVideoUrl(result.payload());
        if (StringUtils.hasText(videoUrl)) {
            completeVideoJob(generateJob, videoUrl);
        }
    }

    private void completeVideoJob(GenerateJob generateJob, String videoUrl) {
        GeneratePrompt prompt = generateJob.getPrompts().stream()
                .filter(generatePrompt -> generatePrompt.getKind() == PromptKind.SCENE)
                .findFirst()
                .orElse(null);

        MediaFile resultMediaFile = MediaFile.builder()
                .user(generateJob.getUser())
                .type(MediaType.VIDEO)
                .filePath(videoUrl)
                .tags("fal.ai")
                .build();
        resultMediaFile.connectGeneration(generateJob, prompt);

        MediaFile savedMediaFile = mediaFileRepository.save(resultMediaFile);
        generateJob.completeJob(savedMediaFile);
    }

    private String extractVideoUrl(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }

        Object video = payload.get("video");
        if (video instanceof Map<?, ?> videoMap) {
            String url = extractString(videoMap, "url");
            if (StringUtils.hasText(url)) {
                return url;
            }
        }

        Object videos = payload.get("videos");
        if (videos instanceof List<?> videoList && !videoList.isEmpty()) {
            Object firstVideo = videoList.getFirst();
            if (firstVideo instanceof Map<?, ?> firstVideoMap) {
                String url = extractString(firstVideoMap, "url");
                if (StringUtils.hasText(url)) {
                    return url;
                }
            }
        }

        for (String key : List.of("video_url", "videoUrl", "output_url", "outputUrl", "url")) {
            String url = extractString(payload, key);
            if (StringUtils.hasText(url)) {
                return url;
            }
        }

        return null;
    }

    private String extractString(Map<?, ?> source, String key) {
        Object value = source.get(key);
        return Objects.toString(value, null);
    }
}
