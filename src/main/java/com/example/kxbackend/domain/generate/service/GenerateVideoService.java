package com.example.kxbackend.domain.generate.service;

import com.example.kxbackend.domain.generate.client.VideoGenerationClient;
import com.example.kxbackend.domain.generate.client.VideoGenerationClientException;
import com.example.kxbackend.domain.generate.client.dto.VideoGenerationCommand;
import com.example.kxbackend.domain.generate.client.dto.VideoGenerationResult;
import com.example.kxbackend.domain.generate.client.dto.VideoGenerationStatusResult;
import com.example.kxbackend.domain.generate.client.dto.VideoGenerationSubmitResult;
import com.example.kxbackend.domain.generate.dto.request.FalWebhookRequestDto;
import com.example.kxbackend.domain.generate.dto.request.ImageToVideoGenerateRequestDto;
import com.example.kxbackend.domain.generate.dto.response.GenerateJobResponseDto;
import com.example.kxbackend.domain.generate.dto.response.GenerateVideoActiveJobResponseDto;
import com.example.kxbackend.domain.generate.dto.response.GenerateJobStatusResponseDto;
import com.example.kxbackend.domain.generate.entity.GenerateJob;
import com.example.kxbackend.domain.generate.entity.GenerateJobReferenceMedia;
import com.example.kxbackend.domain.generate.entity.GeneratePrompt;
import com.example.kxbackend.domain.generate.entity.enums.PromptKind;
import com.example.kxbackend.domain.generate.entity.enums.ReferenceImageType;
import com.example.kxbackend.domain.generate.entity.enums.Status;
import com.example.kxbackend.domain.generate.entity.enums.Type;
import com.example.kxbackend.domain.generate.error.FalErrorMessageMapper;
import com.example.kxbackend.domain.generate.repository.GenerateJobReferenceMediaRepository;
import com.example.kxbackend.domain.generate.repository.GenerateJobRepository;
import com.example.kxbackend.domain.generate.validation.VideoOptionValidator;
import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.media.entity.enums.MediaType;
import com.example.kxbackend.domain.media.repository.MediaFileRepository;
import com.example.kxbackend.domain.user.entity.User;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import com.example.kxbackend.infra.ai.fal.FalProperties;
import com.example.kxbackend.infra.storage.s3.S3PresignedUrlService;
import com.example.kxbackend.infra.storage.service.FalGeneratedVideoStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenerateVideoService {

    private static final int KLING_MAX_REFERENCE_IMAGE_COUNT = 4;
    private static final int SEEDANCE_MAX_REFERENCE_IMAGE_COUNT = 9;
    private static final String DEFAULT_DURATION = "5";
    private static final String DEFAULT_ASPECT_RATIO = "16:9";
    private static final List<Status> PENDING_VIDEO_JOB_STATUSES = List.of(Status.CREATED, Status.SUBMITTED, Status.IN_PROGRESS);
    private static final List<Status> ACTIVE_VIDEO_JOB_STATUSES = List.of(Status.CREATED, Status.SUBMITTED, Status.IN_PROGRESS);
    private static final Pattern STATUS_CODE_PATTERN = Pattern.compile("(?i)status code:?\\s*(\\d{3})");
    private static final String SYNC_SOURCE_WEBHOOK = "WEBHOOK";
    private static final String SYNC_SOURCE_SCHEDULER = "SCHEDULER";

    private final GenerateJobRepository generateJobRepository;
    private final GenerateJobReferenceMediaRepository generateJobReferenceMediaRepository;
    private final MediaFileRepository mediaFileRepository;
    private final VideoGenerationClient videoGenerationClient;
    private final VideoOptionValidator videoOptionValidator;
    private final FalGeneratedVideoStorageService falGeneratedVideoStorageService;
    private final S3PresignedUrlService s3PresignedUrlService;
    private final FalProperties falProperties;

    @Value("${fal.video.created-timeout-minutes:10}")
    private long videoCreatedTimeoutMinutes;

    /**
     * 요청 DTO 기반 이미지-영상 생성 작업 생성
     */
    @Transactional
    public GenerateJob createImageToVideoJob(User user, ImageToVideoGenerateRequestDto request) {
        return createImageToVideoJob(
                user,
                request.startMediaFileId(),
                request.endMediaFileId(),
                request.referenceMediaFileIds(),
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

        if (generateJob.getStatus() == Status.COMPLETED
                || generateJob.getStatus() == Status.FAILED
                || generateJob.getStatus() == Status.CANCELED) {
            return generateJob;
        }

        if (isFailureStatus(request.status()) || StringUtils.hasText(request.error())) {
            String errorMessage = resolveFalWebhookErrorMessage(request);
            log.warn(
                    "fal.ai video generation failed. jobId={}, requestId={}, modelId={}, status={}, errorMessage={}, payloadDetail={}, payloadError={}, payloadMessage={}, payloadKeys={}",
                    generateJob.getId(),
                    request.requestId(),
                    generateJob.getFalModelId(),
                    request.status(),
                    errorMessage,
                    extractPayloadValue(request.payload(), "detail"),
                    extractPayloadValue(request.payload(), "error"),
                    extractPayloadValue(request.payload(), "message"),
                    request.payload() == null ? List.of() : request.payload().keySet()
            );
            generateJob.fail(errorMessage);
            return generateJob;
        }

        String videoUrl = extractVideoUrl(request.payload());
        if (!StringUtils.hasText(videoUrl)) {
            log.warn(
                    "fal.ai video webhook payload did not contain video URL. jobId={}, requestId={}, modelId={}, status={}, payloadKeys={}",
                    generateJob.getId(),
                    request.requestId(),
                    generateJob.getFalModelId(),
                    request.status(),
                    request.payload() == null ? List.of() : request.payload().keySet()
            );
            generateJob.fail("fal.ai webhook payload에서 영상 URL을 찾을 수 없습니다.");
            return generateJob;
        }

        completeVideoJob(generateJob, videoUrl, SYNC_SOURCE_WEBHOOK);
        return generateJob;
    }

    @Transactional
    public GenerateJobResponseDto handleFalWebhookResponse(FalWebhookRequestDto request) {
        GenerateJob generateJob = handleFalWebhook(request);
        return toGenerateJobResponse(generateJob);
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

        VideoGenerationStatusResult falStatus = getFalStatus(generateJob, true);
        syncVideoProgressStatus(generateJob, falStatus);
        MediaFile resultMediaFile = findRepresentativeResultMediaFile(generateJob.getId());
        return GenerateJobStatusResponseDto.from(generateJob, falStatus, resultMediaFile);
    }

    public GenerateJobResponseDto toGenerateJobResponse(GenerateJob generateJob) {
        return GenerateJobResponseDto.from(generateJob, findRepresentativeResultMediaFile(generateJob.getId()));
    }

    @Transactional(readOnly = true)
    public List<GenerateVideoActiveJobResponseDto> getActiveVideoJobs(User user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return generateJobRepository
                .findAllByUser_IdAndTypeAndStatusInOrderByCreatedAtDesc(
                        user.getId(),
                        Type.IMAGE_TO_VIDEO,
                        ACTIVE_VIDEO_JOB_STATUSES
                )
                .stream()
                .map(generateJob -> GenerateVideoActiveJobResponseDto.from(
                        generateJob,
                        generateJobReferenceMediaRepository
                                .findAllByGenerateJob_IdOrderByReferenceTypeAscReferenceOrderAsc(generateJob.getId())
                ))
                .toList();
    }

    /**
     * webhook 누락 또는 사용자의 상태 조회 중단으로 남은 fal 영상 작업을 보정한다.
     */
    @Scheduled(fixedDelayString = "${fal.video.poll-interval-ms:300000}")
    @Transactional
    public void checkPendingVideoGenerationJobs() {
        List<GenerateJob> pendingVideoJobs = generateJobRepository
                .findAllByStatusInAndType(PENDING_VIDEO_JOB_STATUSES, Type.IMAGE_TO_VIDEO);

        if (pendingVideoJobs.isEmpty()) {
            return;
        }

        log.info("진행 중인 fal.ai 영상 생성 작업 {}건 상태 확인 시작", pendingVideoJobs.size());

        for (GenerateJob videoJob : pendingVideoJobs) {
            try {
                syncPendingVideoJob(videoJob, false, SYNC_SOURCE_SCHEDULER);
            } catch (Exception exception) {
                log.error(
                        "fal.ai 영상 생성 작업 동기화 실패. jobId={}, requestId={}, modelId={}",
                        videoJob.getId(),
                        videoJob.getFalRequestId(),
                        videoJob.getFalModelId(),
                        exception
                );
            }
        }
    }

    /**
     * 입력 이미지 URL과 프롬프트를 fal.ai 생성 요청으로 제출
     */
    @Transactional
    public GenerateJob createImageToVideoJob(
            User user,
            Long startMediaFileId,
            Long endMediaFileId,
            List<Long> referenceMediaFileIds,
            String modelId,
            String prompt,
            String webhookUrl,
            Map<String, Object> options
    ) {
        String resolvedModelId = videoOptionValidator.resolveModelId(modelId);
        validateImageToVideoInput(user, resolvedModelId, startMediaFileId, endMediaFileId, referenceMediaFileIds);
        Map<String, Object> resolvedOptions = applyDefaultVideoOptions(resolvedModelId, options);
        videoOptionValidator.validate(resolvedModelId, prompt, resolvedOptions);

        MediaFile startMediaFile = findMediaFile(user, startMediaFileId, "시작 이미지 파일을 찾을 수 없습니다.");
        MediaFile endMediaFile = findMediaFile(user, endMediaFileId, "끝 이미지 파일을 찾을 수 없습니다.");
        List<MediaFile> referenceMediaFiles = findReferenceMediaFiles(user, referenceMediaFileIds);
        MediaFile primaryInputMediaFile = resolvePrimaryInputMediaFile(startMediaFile, endMediaFile, referenceMediaFiles);
        String promptContent = resolvePromptContent(prompt, resolvedOptions);

        GenerateJob generateJob = GenerateJob.builder()
                .user(user)
                .type(Type.IMAGE_TO_VIDEO)
                .status(Status.CREATED)
                .inputMediaFile(primaryInputMediaFile)
                .requestQuality(getOptionValue(resolvedOptions, "quality"))
                .requestAspectRatio(resolveRequestAspectRatio(resolvedOptions))
                .requestResolution(getOptionValue(resolvedOptions, "resolution"))
                .requestDuration(resolveRequestDuration(resolvedOptions))
                .build();
        generateJob.addPrompt(PromptKind.SCENE, 1, promptContent);

        GenerateJob savedJob = generateJobRepository.save(generateJob);
        saveReferenceMedia(savedJob, startMediaFile, endMediaFile, referenceMediaFiles);

        VideoGenerationSubmitResult submitResult;
        try {
            submitResult = videoGenerationClient.submit(
                    new VideoGenerationCommand(
                            resolvedModelId,
                            buildVideoInput(resolvedModelId, startMediaFile, endMediaFile, referenceMediaFiles, prompt, resolvedOptions),
                            resolveWebhookUrl(webhookUrl)
                    )
            );
        } catch (VideoGenerationClientException exception) {
            throw new BusinessException(
                    ErrorCode.AI_PROVIDER_ERROR,
                    buildFalClientErrorMessage("fal.ai submit 요청 실패", exception)
            );
        }

        savedJob.submit(
                submitResult.requestId(),
                resolvedModelId,
                submitResult.statusUrl(),
                submitResult.responseUrl()
        );
        log.info(
                "fal.ai video job submitted. jobId={}, userId={}, requestId={}, modelId={}, duration={}, aspectRatio={}, resolution={}, startMediaFileId={}, endMediaFileId={}, referenceCount={}, statusUrl={}, responseUrl={}",
                savedJob.getId(),
                user.getId(),
                submitResult.requestId(),
                resolvedModelId,
                savedJob.getRequestDuration(),
                savedJob.getRequestAspectRatio(),
                savedJob.getRequestResolution(),
                startMediaFileId,
                endMediaFileId,
                referenceMediaFiles.size(),
                submitResult.statusUrl(),
                submitResult.responseUrl()
        );
        return savedJob;
    }

    private String resolveWebhookUrl(String requestWebhookUrl) {
        if (StringUtils.hasText(falProperties.getWebhookUrl())) {
            return falProperties.getWebhookUrl();
        }
        return requestWebhookUrl;
    }

    private Map<String, Object> applyDefaultVideoOptions(String modelId, Map<String, Object> options) {
        Map<String, Object> resolvedOptions = new LinkedHashMap<>();
        if (options != null) {
            resolvedOptions.putAll(options);
        }
        resolvedOptions.putIfAbsent("duration", DEFAULT_DURATION);
        if (supportsAspectRatioOption(modelId)) {
            resolvedOptions.putIfAbsent("aspect_ratio", DEFAULT_ASPECT_RATIO);
        }
        return resolvedOptions;
    }

    private String resolveRequestAspectRatio(Map<String, Object> options) {
        String aspectRatio = getOptionValue(options, "aspect_ratio");
        return StringUtils.hasText(aspectRatio) ? aspectRatio : DEFAULT_ASPECT_RATIO;
    }

    private String resolveRequestDuration(Map<String, Object> options) {
        Integer multiPromptDuration = resolveMultiPromptDuration(options);
        if (multiPromptDuration != null) {
            return multiPromptDuration.toString();
        }
        String duration = getOptionValue(options, "duration");
        if (StringUtils.hasText(duration)) {
            return duration;
        }
        return null;
    }

    private Integer resolveMultiPromptDuration(Map<String, Object> options) {
        if (options == null) {
            return null;
        }
        Object multiPrompt = options.get("multi_prompt");
        if (!(multiPrompt instanceof List<?> shots)) {
            return null;
        }
        int totalDuration = 0;
        boolean hasDuration = false;
        for (Object item : shots) {
            if (!(item instanceof Map<?, ?> shot)) {
                continue;
            }
            Object value = shot.get("duration");
            if (value == null) {
                continue;
            }
            totalDuration += Integer.parseInt(Objects.toString(value));
            hasDuration = true;
        }
        return hasDuration ? totalDuration : null;
    }

    /**
     * Helper 메서드
     */
    private Map<String, Object> buildVideoInput(
            String modelId,
            MediaFile startMediaFile,
            MediaFile endMediaFile,
            List<MediaFile> referenceMediaFiles,
            String prompt,
            Map<String, Object> options
    ) {
        Map<String, Object> input = new LinkedHashMap<>();
        if (options != null) {
            options.forEach((key, value) -> {
                if (value != null) {
                    input.put(key, value);
                }
            });
        }

        if (isSeedanceReferenceToVideoModel(modelId)) {
            if (!referenceMediaFiles.isEmpty()) {
                input.put("image_urls", referenceMediaFiles.stream()
                        .map(this::resolveFalAccessibleMediaUrl)
                        .toList());
            }
        } else if (isReferenceToVideoModel(modelId)) {
            putMediaFilePath(input, "start_image_url", startMediaFile);
            putMediaFilePath(input, "end_image_url", endMediaFile);
            if (!referenceMediaFiles.isEmpty()) {
                input.put("image_urls", referenceMediaFiles.stream()
                        .map(this::resolveFalAccessibleMediaUrl)
                        .toList());
            }
        } else {
            putMediaFilePath(input, "image_url", startMediaFile);
            putMediaFilePath(input, "end_image_url", endMediaFile);
        }

        if (StringUtils.hasText(prompt)) {
            input.put("prompt", prompt);
        }
        return input;
    }

    private void validateImageToVideoInput(
            User user,
            String modelId,
            Long startMediaFileId,
            Long endMediaFileId,
            List<Long> referenceMediaFileIds
    ) {
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (referenceMediaFileIds != null && referenceMediaFileIds.size() > maxReferenceImageCount(modelId)) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "참조 이미지는 최대 " + maxReferenceImageCount(modelId) + "장까지 사용할 수 있습니다."
            );
        }
        if (isSeedanceReferenceToVideoModel(modelId)) {
            if (isEmpty(referenceMediaFileIds)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Seedance reference-to-video 모델은 참조 이미지가 필요합니다.");
            }
            if (startMediaFileId != null || endMediaFileId != null) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Seedance reference-to-video 모델은 시작/끝 이미지를 지원하지 않습니다.");
            }
            return;
        }
        if (isReferenceToVideoModel(modelId)) {
            if (startMediaFileId == null && endMediaFileId == null && isEmpty(referenceMediaFileIds)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "시작, 끝, 참조 이미지 중 하나 이상이 필요합니다.");
            }
            return;
        }
        if (startMediaFileId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "image-to-video 모델은 시작 이미지가 필요합니다.");
        }
        if (!isEmpty(referenceMediaFileIds)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "image-to-video 모델은 참조 이미지를 지원하지 않습니다.");
        }
    }

    private MediaFile findMediaFile(User user, Long mediaFileId, String notFoundMessage) {
        if (mediaFileId == null) {
            return null;
        }
        MediaFile mediaFile = mediaFileRepository.findByIdAndUserIdAndDeletedFalse(mediaFileId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, notFoundMessage));
        validateMediaFilePath(mediaFile);
        return mediaFile;
    }

    private List<MediaFile> findReferenceMediaFiles(User user, List<Long> referenceMediaFileIds) {
        if (isEmpty(referenceMediaFileIds)) {
            return List.of();
        }
        return referenceMediaFileIds.stream()
                .map(mediaFileId -> findMediaFile(user, mediaFileId, "참조 이미지 파일을 찾을 수 없습니다."))
                .toList();
    }

    private void validateMediaFilePath(MediaFile mediaFile) {
        if (!StringUtils.hasText(mediaFile.getFilePath())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이미지 파일 경로가 필요합니다.");
        }
    }

    private MediaFile resolvePrimaryInputMediaFile(
            MediaFile startMediaFile,
            MediaFile endMediaFile,
            List<MediaFile> referenceMediaFiles
    ) {
        if (startMediaFile != null) {
            return startMediaFile;
        }
        if (!referenceMediaFiles.isEmpty()) {
            return referenceMediaFiles.getFirst();
        }
        return endMediaFile;
    }

    private void saveReferenceMedia(
            GenerateJob generateJob,
            MediaFile startMediaFile,
            MediaFile endMediaFile,
            List<MediaFile> referenceMediaFiles
    ) {
        List<GenerateJobReferenceMedia> references = new java.util.ArrayList<>();
        if (startMediaFile != null) {
            references.add(buildReferenceMedia(generateJob, startMediaFile, ReferenceImageType.START, 1));
        }
        if (endMediaFile != null) {
            references.add(buildReferenceMedia(generateJob, endMediaFile, ReferenceImageType.END, 1));
        }
        for (int index = 0; index < referenceMediaFiles.size(); index++) {
            references.add(buildReferenceMedia(
                    generateJob,
                    referenceMediaFiles.get(index),
                    ReferenceImageType.REFERENCE,
                    index + 1
            ));
        }
        generateJobReferenceMediaRepository.saveAll(references);
    }

    private GenerateJobReferenceMedia buildReferenceMedia(
            GenerateJob generateJob,
            MediaFile mediaFile,
            ReferenceImageType referenceType,
            int referenceOrder
    ) {
        return GenerateJobReferenceMedia.builder()
                .generateJob(generateJob)
                .mediaFile(mediaFile)
                .referenceType(referenceType)
                .referenceOrder(referenceOrder)
                .build();
    }

    private void putMediaFilePath(Map<String, Object> input, String key, MediaFile mediaFile) {
        if (mediaFile != null) {
            input.put(key, resolveFalAccessibleMediaUrl(mediaFile));
        }
    }

    private String resolveFalAccessibleMediaUrl(MediaFile mediaFile) {
        String filePath = mediaFile.getFilePath();
        if (isExternalUrl(filePath)) {
            return filePath;
        }
        return s3PresignedUrlService.createReadUrl(filePath).url();
    }

    private boolean isExternalUrl(String filePath) {
        return filePath.startsWith("http://") || filePath.startsWith("https://");
    }

    private boolean isReferenceToVideoModel(String modelId) {
        return videoOptionValidator.isReferenceToVideoModel(modelId);
    }

    private boolean isSeedanceReferenceToVideoModel(String modelId) {
        return videoOptionValidator.isSeedanceReferenceToVideoModel(modelId);
    }

    private boolean supportsAspectRatioOption(String modelId) {
        return isReferenceToVideoModel(modelId) || isSeedanceReferenceToVideoModel(modelId);
    }

    private int maxReferenceImageCount(String modelId) {
        if (isSeedanceReferenceToVideoModel(modelId)) {
            return SEEDANCE_MAX_REFERENCE_IMAGE_COUNT;
        }
        return KLING_MAX_REFERENCE_IMAGE_COUNT;
    }

    private boolean isEmpty(List<?> values) {
        return values == null || values.isEmpty();
    }

    private boolean isFailureStatus(String status) {
        return "FAILED".equalsIgnoreCase(status)
                || "ERROR".equalsIgnoreCase(status);
    }

    private String resolveFalWebhookErrorMessage(FalWebhookRequestDto request) {
        FalPayloadDetail payloadDetail = extractPayloadDetail(request.payload());
        String statusCode = extractStatusCode(request.error());
        if (payloadDetail != null && StringUtils.hasText(payloadDetail.message())) {
            return FalErrorMessageMapper.toUserMessage(payloadDetail.type(), payloadDetail.message(), statusCode);
        }

        Object payloadMessage = extractPayloadValue(request.payload(), "message");
        if (payloadMessage != null && StringUtils.hasText(Objects.toString(payloadMessage, null))) {
            return FalErrorMessageMapper.toUserMessage(extractPayloadErrorType(request.payload()), Objects.toString(payloadMessage, null), statusCode);
        }

        Object payloadError = extractPayloadValue(request.payload(), "error");
        if (payloadError != null && StringUtils.hasText(Objects.toString(payloadError, null))) {
            return FalErrorMessageMapper.toUserMessage(extractPayloadErrorType(request.payload()), Objects.toString(payloadError, null), statusCode);
        }

        if (StringUtils.hasText(request.error())) {
            return request.error();
        }
        return request.status();
    }

    private FalPayloadDetail extractPayloadDetail(Map<String, Object> payload) {
        Object detail = extractPayloadValue(payload, "detail");
        if (detail instanceof List<?> details && !details.isEmpty()) {
            Object firstDetail = details.getFirst();
            if (firstDetail instanceof Map<?, ?> detailMap) {
                return new FalPayloadDetail(
                        Objects.toString(detailMap.get("type"), null),
                        Objects.toString(detailMap.get("msg"), null)
                );
            }
            return new FalPayloadDetail(null, Objects.toString(firstDetail, null));
        }
        if (detail instanceof Map<?, ?> detailMap) {
            return new FalPayloadDetail(
                    Objects.toString(detailMap.get("type"), null),
                    Objects.toString(detailMap.get("msg"), null)
            );
        }
        return null;
    }

    private String extractStatusCode(String error) {
        if (!StringUtils.hasText(error)) {
            return null;
        }
        Matcher matcher = STATUS_CODE_PATTERN.matcher(error);
        return matcher.find() ? matcher.group(1) : null;
    }

    private Object extractPayloadValue(Map<String, Object> payload, String key) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }
        return payload.get(key);
    }

    private String extractPayloadErrorType(Map<String, Object> payload) {
        Object errorType = extractPayloadValue(payload, "error_type");
        if (errorType != null && StringUtils.hasText(Objects.toString(errorType, null))) {
            return Objects.toString(errorType, null);
        }
        Object type = extractPayloadValue(payload, "type");
        return type == null ? null : Objects.toString(type, null);
    }

    private VideoGenerationStatusResult syncPendingVideoJob(GenerateJob generateJob, boolean withLogs, String source) {
        if (generateJob.getStatus() == Status.COMPLETED
                || generateJob.getStatus() == Status.FAILED
                || generateJob.getStatus() == Status.CANCELED) {
            return null;
        }

        if (!hasFalRequestInfo(generateJob)) {
            failStaleCreatedVideoJob(generateJob);
            return null;
        }

        VideoGenerationStatusResult falStatus = getFalStatus(generateJob, withLogs);
        if (falStatus == null) {
            return null;
        }

        if (isCompletedStatus(falStatus.status())) {
            log.info(
                    "fal.ai video job completed remotely. source={}, jobId={}, requestId={}, modelId={}, responseUrl={}",
                    source,
                    generateJob.getId(),
                    generateJob.getFalRequestId(),
                    generateJob.getFalModelId(),
                    falStatus.responseUrl()
            );
            completeVideoJobFromFalResult(generateJob, falStatus, source);
            return falStatus;
        }

        if (isFailureStatus(falStatus.status()) || StringUtils.hasText(falStatus.error())) {
            log.warn(
                    "fal.ai video job failed remotely. source={}, jobId={}, requestId={}, modelId={}, falStatus={}, errorType={}, error={}",
                    source,
                    generateJob.getId(),
                    generateJob.getFalRequestId(),
                    generateJob.getFalModelId(),
                    falStatus.status(),
                    falStatus.errorType(),
                    falStatus.error()
            );
            generateJob.fail(resolveFalStatusErrorMessage(falStatus));
            return falStatus;
        }

        if ("IN_PROGRESS".equalsIgnoreCase(falStatus.status()) && generateJob.getStatus() == Status.SUBMITTED) {
            generateJob.progress();
        }

        return falStatus;
    }

    private boolean hasFalRequestInfo(GenerateJob generateJob) {
        return StringUtils.hasText(generateJob.getFalModelId())
                && StringUtils.hasText(generateJob.getFalRequestId());
    }

    private void failStaleCreatedVideoJob(GenerateJob generateJob) {
        if (generateJob.getStatus() != Status.CREATED || generateJob.getCreatedAt() == null) {
            return;
        }
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(videoCreatedTimeoutMinutes);
        if (generateJob.getCreatedAt().isAfter(timeoutThreshold)) {
            return;
        }
        generateJob.fail("fal.ai 영상 생성 요청 정보가 없어 작업을 종료했습니다.");
    }

    private void completeVideoJobFromFalResult(GenerateJob generateJob, VideoGenerationStatusResult falStatus, String source) {
        VideoGenerationResult result = getFalResult(generateJob, falStatus);
        String videoUrl = result == null ? null : extractVideoUrl(result.payload());
        if (!StringUtils.hasText(videoUrl)) {
            generateJob.fail("fal.ai 완료 결과에서 영상 URL을 찾을 수 없습니다.");
            return;
        }
        completeVideoJob(generateJob, videoUrl, source);
    }

    private void syncVideoProgressStatus(GenerateJob generateJob, VideoGenerationStatusResult falStatus) {
        if (falStatus == null || generateJob.getStatus() != Status.SUBMITTED) {
            return;
        }
        if ("IN_PROGRESS".equalsIgnoreCase(falStatus.status())) {
            generateJob.progress();
        }
    }

    private VideoGenerationStatusResult getFalStatus(GenerateJob generateJob, boolean withLogs) {
        if (!StringUtils.hasText(generateJob.getFalModelId()) || !StringUtils.hasText(generateJob.getFalRequestId())) {
            return null;
        }
        if (generateJob.getStatus() == Status.COMPLETED
                || generateJob.getStatus() == Status.FAILED
                || generateJob.getStatus() == Status.CANCELED) {
            return null;
        }
        if (StringUtils.hasText(generateJob.getFalStatusUrl())) {
            try {
                return videoGenerationClient.getStatusByUrl(generateJob.getFalStatusUrl(), withLogs);
            } catch (VideoGenerationClientException exception) {
                throw new BusinessException(
                        ErrorCode.AI_PROVIDER_ERROR,
                        buildFalClientErrorMessage("fal.ai status 조회 실패", exception)
                );
            }
        }
        try {
            return videoGenerationClient.getStatus(generateJob.getFalModelId(), generateJob.getFalRequestId(), withLogs);
        } catch (VideoGenerationClientException exception) {
            throw new BusinessException(
                    ErrorCode.AI_PROVIDER_ERROR,
                    buildFalClientErrorMessage("fal.ai status 조회 실패", exception)
            );
        }
    }

    private VideoGenerationResult getFalResult(GenerateJob generateJob, VideoGenerationStatusResult falStatus) {
        if (StringUtils.hasText(falStatus.responseUrl())) {
            try {
                return videoGenerationClient.getResultByUrl(falStatus.responseUrl());
            } catch (VideoGenerationClientException exception) {
                throw new BusinessException(
                        ErrorCode.AI_PROVIDER_ERROR,
                        buildFalClientErrorMessage("fal.ai result 조회 실패", exception)
                );
            }
        }
        if (StringUtils.hasText(generateJob.getFalResponseUrl())) {
            try {
                return videoGenerationClient.getResultByUrl(generateJob.getFalResponseUrl());
            } catch (VideoGenerationClientException exception) {
                throw new BusinessException(
                        ErrorCode.AI_PROVIDER_ERROR,
                        buildFalClientErrorMessage("fal.ai result 조회 실패", exception)
                );
            }
        }
        try {
            return videoGenerationClient.getResult(generateJob.getFalModelId(), generateJob.getFalRequestId());
        } catch (VideoGenerationClientException exception) {
            throw new BusinessException(
                    ErrorCode.AI_PROVIDER_ERROR,
                    buildFalClientErrorMessage("fal.ai result 조회 실패", exception)
            );
        }
    }

    private boolean isCompletedStatus(String status) {
        return "COMPLETED".equalsIgnoreCase(status);
    }

    private String resolveFalStatusErrorMessage(VideoGenerationStatusResult falStatus) {
        if (StringUtils.hasText(falStatus.error())) {
            return FalErrorMessageMapper.toUserMessage(falStatus.errorType(), falStatus.error(), null);
        }
        return falStatus.status();
    }

    private String buildFalClientErrorMessage(String prefix, VideoGenerationClientException exception) {
        if (StringUtils.hasText(exception.getResponseBody())) {
            return prefix + ": HTTP " + exception.getStatusCode() + " - " + exception.getResponseBody();
        }
        return prefix + ": HTTP " + exception.getStatusCode();
    }

    private void completeVideoJob(GenerateJob generateJob, String videoUrl, String source) {
        GenerateJob lockedJob = generateJobRepository.findByIdForUpdate(generateJob.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "영상 생성 작업을 찾을 수 없습니다."));

        if (lockedJob.getStatus() == Status.COMPLETED || lockedJob.getStatus() == Status.CANCELED) {
            log.info(
                    "fal.ai video result save skipped by terminal status. source={}, jobId={}, requestId={}, status={}",
                    source,
                    lockedJob.getId(),
                    lockedJob.getFalRequestId(),
                    lockedJob.getStatus()
            );
            return;
        }
        MediaFile existingResultMediaFile = findRepresentativeResultMediaFile(lockedJob.getId());
        if (existingResultMediaFile != null) {
            log.info(
                    "fal.ai video result save skipped by existing media. source={}, jobId={}, requestId={}, existingMediaFileId={}, existingFilePath={}",
                    source,
                    lockedJob.getId(),
                    lockedJob.getFalRequestId(),
                    existingResultMediaFile.getId(),
                    existingResultMediaFile.getFilePath()
            );
            lockedJob.completeJob();
            return;
        }

        log.debug(
                "fal.ai video result save started. source={}, jobId={}, requestId={}, modelId={}, userId={}",
                source,
                lockedJob.getId(),
                lockedJob.getFalRequestId(),
                lockedJob.getFalModelId(),
                lockedJob.getUser().getId()
        );
        String savedVideoPath = falGeneratedVideoStorageService.saveGeneratedVideo(
                lockedJob.getUser().getId(),
                videoUrl
        );
        GeneratePrompt prompt = lockedJob.getPrompts().stream()
                .filter(generatePrompt -> generatePrompt.getKind() == PromptKind.SCENE)
                .findFirst()
                .orElse(null);

        MediaFile resultMediaFile = MediaFile.builder()
                .user(lockedJob.getUser())
                .type(MediaType.VIDEO)
                .filePath(savedVideoPath)
                .model(lockedJob.getFalModelId())
                .quality(lockedJob.getRequestQuality())
                .aspectRatio(lockedJob.getRequestAspectRatio())
                .resolution(lockedJob.getRequestResolution())
                .duration(lockedJob.getRequestDuration())
                .tags("fal.ai")
                .build();
        resultMediaFile.connectGeneration(lockedJob, prompt);

        mediaFileRepository.save(resultMediaFile);
        lockedJob.completeJob();
        log.info(
                "fal.ai video result saved. source={}, jobId={}, requestId={}, mediaFileId={}, filePath={}, modelId={}, duration={}",
                source,
                lockedJob.getId(),
                lockedJob.getFalRequestId(),
                resultMediaFile.getId(),
                savedVideoPath,
                lockedJob.getFalModelId(),
                lockedJob.getRequestDuration()
        );
    }

    private MediaFile findRepresentativeResultMediaFile(Long generateJobId) {
        List<MediaFile> resultMediaFiles =
                mediaFileRepository.findAllByGenerateJob_IdAndDeletedFalseOrderByIdAsc(generateJobId);
        return resultMediaFiles.isEmpty() ? null : resultMediaFiles.getFirst();
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

    private String getOptionValue(Map<String, Object> options, String key) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        Object value = options.get(key);
        return value == null ? null : Objects.toString(value, null);
    }

    private String resolvePromptContent(String prompt, Map<String, Object> options) {
        if (StringUtils.hasText(prompt)) {
            return prompt;
        }
        if (options == null) {
            return "";
        }
        Object multiPrompt = options.get("multi_prompt");
        if (!(multiPrompt instanceof List<?> shots)) {
            return "";
        }
        return shots.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(shot -> Objects.toString(shot.get("prompt"), ""))
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n\n"));
    }

    private record FalPayloadDetail(String type, String message) {
    }
}
