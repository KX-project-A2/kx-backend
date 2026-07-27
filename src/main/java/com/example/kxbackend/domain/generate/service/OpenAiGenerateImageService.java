package com.example.kxbackend.domain.generate.service;

import com.example.kxbackend.domain.generate.dto.request.CharacterConceptSheetRequestDto;
import com.example.kxbackend.domain.generate.dto.request.OpenAiGenerateImageRequestDto;
import com.example.kxbackend.domain.generate.dto.response.OpenAiActiveImageJobResponseDto;
import com.example.kxbackend.domain.generate.dto.response.OpenAiGenerateImageJobResponseDto;
import com.example.kxbackend.domain.generate.entity.GenerateJob;
import com.example.kxbackend.domain.generate.entity.GeneratePrompt;
import com.example.kxbackend.domain.generate.entity.OpenAiImageReference;
import com.example.kxbackend.domain.generate.entity.OpenAiImageGenerateJobOption;
import com.example.kxbackend.domain.generate.entity.enums.ImageGenerationPurpose;
import com.example.kxbackend.domain.generate.entity.enums.PromptKind;
import com.example.kxbackend.domain.generate.entity.enums.ReferenceImageType;
import com.example.kxbackend.domain.generate.entity.enums.Status;
import com.example.kxbackend.domain.generate.entity.enums.Type;
import com.example.kxbackend.domain.generate.prompt.CharacterConceptArtPromptBuilder;
import com.example.kxbackend.domain.generate.repository.OpenAiImageGenerateJobOptionRepository;
import com.example.kxbackend.domain.generate.repository.OpenAiImageGenerateJobRepository;
import com.example.kxbackend.domain.generate.repository.OpenAiImageReferenceRepository;
import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.media.entity.enums.MediaType;
import com.example.kxbackend.domain.media.repository.MediaFileRepository;
import com.example.kxbackend.domain.user.entity.User;
import com.example.kxbackend.domain.user.repository.UserRepository;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import com.example.kxbackend.infra.ai.anthropic.ClaudePromptCorrectionClient;
import com.example.kxbackend.infra.ai.openai.OpenAiBatchClient;
import com.example.kxbackend.infra.ai.openai.OpenAiImageBatchResultClient;
import com.example.kxbackend.infra.ai.openai.OpenAiImageBatchResultClient.OpenAiBatchStatusResponse;
import com.example.kxbackend.infra.ai.openai.OpenAiReferenceImageClient;
import com.example.kxbackend.infra.storage.service.ImageUploadStorageService;
import com.example.kxbackend.infra.storage.service.OpenAiGeneratedImageStorageService;
import com.example.kxbackend.infra.storage.validation.UploadedImageFileValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * OpenAI Images API 기반 이미지 생성 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OpenAiGenerateImageService {

    private static final int MAX_REFERENCE_IMAGE_COUNT = 8;
    private static final List<Status> PENDING_IMAGE_JOB_STATUSES = List.of(Status.SUBMITTED, Status.IN_PROGRESS);
    private static final List<Status> ACTIVE_IMAGE_JOB_STATUSES = List.of(Status.CREATED, Status.SUBMITTED, Status.IN_PROGRESS);

    private final OpenAiBatchClient openAiBatchClient;
    private final OpenAiImageBatchResultClient openAiImageBatchResultClient;
    private final OpenAiImageGenerationProcessor openAiImageGenerationProcessor;
    private final ClaudePromptCorrectionClient claudePromptCorrectionClient;
    private final OpenAiReferenceImageClient openAiReferenceImageClient;
    private final OpenAiImageGenerateJobRepository openAiImageGenerateJobRepository;
    private final OpenAiImageGenerateJobOptionRepository openAiImageGenerateJobOptionRepository;
    private final OpenAiImageReferenceRepository openAiImageReferenceRepository;
    private final MediaFileRepository mediaFileRepository;
    private final UserRepository userRepository;
    private final OpenAiGeneratedImageStorageService openAiGeneratedImageStorageService;
    private final ImageUploadStorageService imageUploadStorageService;
    private final CharacterConceptArtPromptBuilder characterConceptArtPromptBuilder;
    private final ObjectMapper objectMapper;

    @Value("${openai.image.model:gpt-image-2}")
    private String imageModel;

    @Value("${openai.image.size:auto}")
    private String defaultImageSize;

    @Value("${openai.image.character-sheet.size:1536x1024}")
    private String defaultCharacterSheetSize;

    @Value("${openai.image.quality:standard}")
    private String defaultImageQuality;

    /**
     * OpenAI Images API로 이미지를 생성한다. 레퍼런스 이미지는 0~8장까지 첨부할 수 있다.
     */
    @Transactional
    public OpenAiGenerateImageJobResponseDto requestImageGeneration(
            Long userId,
            OpenAiGenerateImageRequestDto request,
            List<MultipartFile> referenceFiles
    ) {
        List<MultipartFile> references = buildReferenceUploads(referenceFiles);
        validateReferenceImages(references);
        ImageGenerationPurpose purpose = request.purpose();
        String prompt = resolvePrompt(request.prompt(), request.promptCorrectionEnabled());
        String generationPrompt = appendPurposeInstructions(prompt, purpose);
        if (!references.isEmpty()) {
            generationPrompt = appendReferenceInstructions(generationPrompt, references.size());
        }

        return submitImageGenerationJob(
                userId,
                prompt,
                generationPrompt,
                resolveImageCount(request.imageCount()),
                resolveSize(request.size()),
                resolveQuality(request.quality()),
                purpose,
                references
        );
    }

    /**
     * 구조화된 캐릭터 데이터로 공식 캐릭터 설정표(Concept Art Sheet) 생성을 요청한다.
     */
    @Transactional
    public OpenAiGenerateImageJobResponseDto requestCharacterConceptSheet(
            Long userId,
            CharacterConceptSheetRequestDto request
    ) {
        String prompt = characterConceptArtPromptBuilder.build(request);
        return submitImageGenerationJob(
                userId,
                prompt,
                prompt,
                resolveImageCount(request.imageCount()),
                resolveCharacterSheetSize(request.size()),
                resolveQuality(request.quality()),
                ImageGenerationPurpose.CHARACTER,
                List.of()
        );
    }

    private OpenAiGenerateImageJobResponseDto submitImageGenerationJob(
            Long userId,
            String storedPrompt,
            String generationPrompt,
            int imageCount,
            String size,
            String quality,
            ImageGenerationPurpose purpose,
            List<MultipartFile> referenceUploads
    ) {
        User user = getUser(userId);

        GenerateJob imageJob = GenerateJob.builder()
                .user(user)
                .type(Type.TEXT_TO_IMAGE)
                .status(Status.CREATED)
                .build();
        imageJob.addPrompt(PromptKind.IMAGE, 1, storedPrompt);
        openAiImageGenerateJobRepository.save(imageJob);

        List<OpenAiImageReference> references = saveReferenceImages(imageJob, user, referenceUploads);

        OpenAiImageGenerateJobOption jobOption =
                OpenAiImageGenerateJobOption.of(imageJob, imageCount, size, quality, purpose);
        openAiImageGenerateJobOptionRepository.save(jobOption);

        imageJob.startSynchronous();

        Long jobId = imageJob.getId();
        String openAiQuality = toOpenAiQuality(quality);
        triggerAfterCommit(() -> openAiImageGenerationProcessor.process(
                jobId, generationPrompt, imageCount, size, openAiQuality));

        log.info("OpenAI 이미지 생성 요청 접수. jobId={}, imageCount={}, referenceCount={}",
                jobId, imageCount, references.size());

        return buildResponse(imageJob, jobOption);
    }

    /**
     * OpenAI 이미지 생성 작업 상세를 조회한다.
     */
    public OpenAiGenerateImageJobResponseDto getImageJob(Long userId, Long jobId) {
        GenerateJob imageJob = getOwnedImageJob(userId, jobId);
        OpenAiImageGenerateJobOption jobOption = openAiImageGenerateJobOptionRepository.findById(jobId).orElse(null);
        return buildResponse(imageJob, jobOption);
    }

    public List<OpenAiActiveImageJobResponseDto> getActiveImageJobs(Long userId) {
        return openAiImageGenerateJobRepository
                .findAllByUser_IdAndTypeAndStatusInOrderByCreatedAtDesc(
                        userId,
                        Type.TEXT_TO_IMAGE,
                        ACTIVE_IMAGE_JOB_STATUSES
                )
                .stream()
                .map(imageJob -> OpenAiActiveImageJobResponseDto.from(
                        imageJob,
                        openAiImageGenerateJobOptionRepository.findById(imageJob.getId()).orElse(null),
                        openAiImageReferenceRepository.findAllByGenerateJob_IdOrderByReferenceOrderAsc(imageJob.getId())
                ))
                .toList();
    }

    /**
     * 진행 중인 OpenAI 이미지 생성 배치 작업 상태를 주기적으로 조회한다.
     */
    @Scheduled(fixedDelayString = "${openai.batch.poll-interval-ms:600000}")
    @Transactional
    public void checkPendingImageGenerationJobs() {
        List<GenerateJob> pendingImageJobs = openAiImageGenerateJobRepository
                .findAllByStatusInAndType(PENDING_IMAGE_JOB_STATUSES, Type.TEXT_TO_IMAGE);

        if (pendingImageJobs.isEmpty()) {
            return;
        }

        log.info("진행 중인 OpenAI 이미지 생성 작업 {}건 상태 확인 시작", pendingImageJobs.size());

        for (GenerateJob imageJob : pendingImageJobs) {
            if (imageJob.getFalRequestId() == null || imageJob.getFalRequestId().isBlank()) {
                continue;
            }
            try {
                processPendingImageGenerationJob(imageJob);
            } catch (Exception exception) {
                log.error("OpenAI 이미지 생성 작업 처리 실패. jobId={}, batchId={}",
                        imageJob.getId(), imageJob.getFalRequestId(), exception);
                imageJob.fail("OpenAI 이미지 생성 작업 처리 중 오류가 발생했습니다.");
                deleteOpenAiReferenceFiles(imageJob);
            }
        }
    }

    private void processPendingImageGenerationJob(GenerateJob imageJob) {
        OpenAiBatchStatusResponse statusResponse = openAiImageBatchResultClient
                .parseBatchStatus(openAiBatchClient.checkBatchStatus(imageJob.getFalRequestId()));
        String status = statusResponse.status();

        if ("completed".equalsIgnoreCase(status)) {
            completeImageGenerationJob(imageJob, statusResponse.outputFileId(), statusResponse.errorFileId());
            return;
        }

        if ("failed".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status) || "expired".equalsIgnoreCase(status)) {
            imageJob.fail("OpenAI 이미지 생성 작업이 실패했습니다. status=" + status);
            deleteOpenAiReferenceFiles(imageJob);
            return;
        }

        imageJob.progress();
    }

    private void completeImageGenerationJob(GenerateJob imageJob, String outputFileId, String errorFileId) {
        if (outputFileId == null || outputFileId.isBlank()) {
            String errorMessage = errorFileId != null && !errorFileId.isBlank()
                    ? openAiImageBatchResultClient.extractErrorMessage(errorFileId)
                    : null;
            imageJob.fail(errorMessage != null
                    ? "OpenAI 이미지 생성 요청이 거부되었습니다: " + errorMessage
                    : "완료된 작업의 결과 파일 ID가 없습니다.");
            deleteOpenAiReferenceFiles(imageJob);
            return;
        }

        String jsonlContent = openAiImageBatchResultClient.downloadFileContent(outputFileId);
        List<byte[]> imageBytesList = openAiImageBatchResultClient.extractAllImageBytes(jsonlContent);
        saveGeneratedImageResults(imageJob, imageBytesList);
    }

    private void saveGeneratedImageResults(GenerateJob imageJob, List<byte[]> imageBytesList) {
        GeneratePrompt prompt = imageJob.getPrompts().getFirst();
        OpenAiImageGenerateJobOption jobOption =
                openAiImageGenerateJobOptionRepository.findById(imageJob.getId()).orElse(null);
        List<MediaFile> savedMediaFiles = new ArrayList<>();

        for (byte[] imageBytes : imageBytesList) {
            String savedPath = openAiGeneratedImageStorageService.saveGeneratedImage(imageJob.getUser().getId(), imageBytes);

            MediaFile mediaFile = MediaFile.builder()
                    .user(imageJob.getUser())
                    .type(MediaType.IMAGE)
                    .filePath(savedPath)
                    .model(imageModel)
                    .quality(jobOption != null ? jobOption.getQuality() : null)
                    .resolution(jobOption != null ? jobOption.getSize() : null)
                    .purpose(jobOption != null ? jobOption.getPurpose() : null)
                    .tags("openai")
                    .build();
            mediaFile.connectGeneration(imageJob, prompt);
            savedMediaFiles.add(mediaFileRepository.save(mediaFile));
        }

        imageJob.completeJob();
        deleteOpenAiReferenceFiles(imageJob);
        log.info("OpenAI 이미지 생성 완료. jobId={}, imageCount={}", imageJob.getId(), savedMediaFiles.size());
    }

    /**
     * 현재 트랜잭션이 커밋된 후에 작업을 실행하도록 등록한다.
     * 커밋 전에 비동기 스레드가 작업을 조회하지 못하는 문제를 방지한다.
     */
    private void triggerAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private OpenAiGenerateImageJobResponseDto buildResponse(GenerateJob imageJob, OpenAiImageGenerateJobOption jobOption) {
        List<MediaFile> resultMediaFiles = mediaFileRepository.findAllByGenerateJob_IdAndDeletedFalseOrderByIdAsc(imageJob.getId());
        List<OpenAiImageReference> references =
                openAiImageReferenceRepository.findAllByGenerateJob_IdOrderByReferenceOrderAsc(imageJob.getId());
        return OpenAiGenerateImageJobResponseDto.from(imageJob, jobOption, resultMediaFiles, references);
    }

    private List<MultipartFile> buildReferenceUploads(List<MultipartFile> referenceFiles) {
        return safeFiles(referenceFiles).stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
    }

    private List<MultipartFile> safeFiles(List<MultipartFile> files) {
        return files == null ? Collections.emptyList() : files;
    }

    private void validateReferenceImages(List<MultipartFile> references) {
        if (references.size() > MAX_REFERENCE_IMAGE_COUNT) {
            throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "레퍼런스 이미지는 최대 8장까지 첨부할 수 있습니다."
            );
        }

        for (MultipartFile file : references) {
            UploadedImageFileValidator.validate(file, "레퍼런스 이미지");
        }
    }

    private String appendPurposeInstructions(String prompt, ImageGenerationPurpose purpose) {
        if (purpose == ImageGenerationPurpose.CHARACTER) {
            return prompt + """

                    [IMAGE GENERATION PURPOSE: CHARACTER]
                    Prioritize the character's identity, design, pose, expression, outfit, and visual consistency.
                    Arrange front, side, and back views of the same character side by side in one image.
                    Use a clean white or neutral studio background with no scenic backdrop and no cast shadows from the environment.
                    Do not include any readable text, labels, logos, captions, UI, or watermarks in the image.
                    """;
        }
        return prompt + """

                [IMAGE GENERATION PURPOSE: BACKGROUND]
                Prioritize the environment, composition, architecture or landscape, atmosphere, lighting, and spatial depth.
                Keep any characters secondary unless the user's prompt explicitly requires otherwise.
                """;
    }

    private String appendReferenceInstructions(String prompt, int referenceCount) {
        return prompt + """

                [REFERENCE IMAGE INSTRUCTIONS]
                %d reference image(s) are attached.
                Use them as visual guidance for identity, style, composition, color, lighting, and mood as appropriate,
                and create one cohesive new image that follows the user's prompt.
                Do not copy the references verbatim.
                """.formatted(referenceCount);
    }

    private List<OpenAiImageReference> saveReferenceImages(
            GenerateJob imageJob,
            User user,
            List<MultipartFile> referenceUploads
    ) {
        if (referenceUploads.isEmpty()) {
            return List.of();
        }

        List<OpenAiImageReference> references = new ArrayList<>();
        try {
            for (int index = 0; index < referenceUploads.size(); index++) {
                MultipartFile file = referenceUploads.get(index);
                byte[] content = file.getBytes();
                String savedPath = imageUploadStorageService.uploadMediaImage(user.getId(), file);

                MediaFile mediaFile = mediaFileRepository.save(
                        MediaFile.builder()
                                .user(user)
                                .type(MediaType.IMAGE)
                                .filePath(savedPath)
                                .tags("reference")
                                .build()
                );

                String openAiFileId = openAiReferenceImageClient.uploadReferenceImage(
                        content,
                        buildOpenAiReferenceFileName(index, file.getContentType()),
                        file.getContentType()
                );

                OpenAiImageReference reference = OpenAiImageReference.builder()
                        .generateJob(imageJob)
                        .mediaFile(mediaFile)
                        .referenceType(ReferenceImageType.REFERENCE)
                        .referenceOrder(index + 1)
                        .openAiFileId(openAiFileId)
                        .build();
                references.add(reference);
                openAiImageReferenceRepository.save(reference);
            }
            return List.copyOf(references);
        } catch (IOException exception) {
            deleteOpenAiReferenceFiles(references);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "레퍼런스 이미지 파일을 읽는 데 실패했습니다.");
        } catch (RuntimeException exception) {
            deleteOpenAiReferenceFiles(references);
            throw exception;
        }
    }

    private String buildOpenAiReferenceFileName(int index, String contentType) {
        String extension = switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            default -> "png";
        };
        return "reference-" + (index + 1) + "-" + UUID.randomUUID() + "." + extension;
    }

    private void deleteOpenAiReferenceFiles(GenerateJob imageJob) {
        deleteOpenAiReferenceFiles(
                openAiImageReferenceRepository.findAllByGenerateJob_IdOrderByReferenceOrderAsc(imageJob.getId())
        );
    }

    private void deleteOpenAiReferenceFiles(List<OpenAiImageReference> references) {
        references.forEach(reference ->
                openAiReferenceImageClient.deleteReferenceImage(reference.getOpenAiFileId()));
    }

    private GenerateJob getOwnedImageJob(Long userId, Long jobId) {
        GenerateJob imageJob = openAiImageGenerateJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "이미지 생성 작업을 찾을 수 없습니다."));

        if (!imageJob.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "해당 이미지 생성 작업에 접근할 수 없습니다.");
        }

        if (imageJob.getType() != Type.TEXT_TO_IMAGE) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "이미지 생성 작업을 찾을 수 없습니다.");
        }

        return imageJob;
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    private int resolveImageCount(Integer imageCount) {
        return imageCount == null ? 1 : imageCount;
    }

    private String resolvePrompt(String prompt, Boolean promptCorrectionEnabled) {
        return Boolean.TRUE.equals(promptCorrectionEnabled)
                ? claudePromptCorrectionClient.correct(prompt)
                : prompt;
    }

    private String resolveSize(String size) {
        return size == null || size.isBlank() ? defaultImageSize : size;
    }

    private String resolveCharacterSheetSize(String size) {
        if (size == null || size.isBlank() || "auto".equalsIgnoreCase(size)) {
            return defaultCharacterSheetSize;
        }
        return size;
    }

    private String resolveQuality(String quality) {
        return quality == null || quality.isBlank() ? defaultImageQuality : quality;
    }

    /**
     * UI 품질 값을 OpenAI가 지원하는 값(low/medium/high/auto)으로 변환한다.
     */
    private String toOpenAiQuality(String quality) {
        return "standard".equalsIgnoreCase(quality) ? "medium" : quality;
    }

    private String convertPromptToJsonl(String prompt, int imageCount, String size, String quality) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", imageModel);
            body.put("prompt", prompt);
            body.put("n", imageCount);
            body.put("size", size);
            body.put("quality", toOpenAiQuality(quality));

            Map<String, Object> requestLine = new LinkedHashMap<>();
            requestLine.put("custom_id", "openai-image-" + UUID.randomUUID());
            requestLine.put("method", "POST");
            requestLine.put("url", "/v1/images/generations");
            requestLine.put("body", body);

            return objectMapper.writeValueAsString(requestLine) + "\n";
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "배치 요청 JSONL 생성에 실패했습니다.");
        }
    }

    private String convertReferencePromptToJsonl(
            String prompt,
            int imageCount,
            String size,
            String quality,
            List<OpenAiImageReference> references
    ) {
        try {
            List<Map<String, String>> images = references.stream()
                    .map(reference -> Map.of("file_id", reference.getOpenAiFileId()))
                    .toList();

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", imageModel);
            body.put("images", images);
            body.put("prompt", prompt);
            body.put("n", imageCount);
            body.put("size", size);
            body.put("quality", toOpenAiQuality(quality));

            Map<String, Object> requestLine = new LinkedHashMap<>();
            requestLine.put("custom_id", "openai-image-reference-" + UUID.randomUUID());
            requestLine.put("method", "POST");
            requestLine.put("url", "/v1/images/edits");
            requestLine.put("body", body);

            return objectMapper.writeValueAsString(requestLine) + "\n";
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "레퍼런스 이미지 배치 요청 JSONL 생성에 실패했습니다.");
        }
    }
}
