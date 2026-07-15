package com.example.kxbackend.domain.generate.service;

import com.example.kxbackend.domain.generate.dto.request.CharacterConceptSheetRequestDto;
import com.example.kxbackend.domain.generate.dto.request.OpenAiGenerateImageRequestDto;
import com.example.kxbackend.domain.generate.dto.response.OpenAiGenerateImageJobResponseDto;
import com.example.kxbackend.domain.generate.entity.GenerateJob;
import com.example.kxbackend.domain.generate.entity.GeneratePrompt;
import com.example.kxbackend.domain.generate.entity.OpenAiImageGenerateJobOption;
import com.example.kxbackend.domain.generate.entity.enums.PromptKind;
import com.example.kxbackend.domain.generate.entity.enums.Status;
import com.example.kxbackend.domain.generate.entity.enums.Type;
import com.example.kxbackend.domain.generate.prompt.CharacterConceptArtPromptBuilder;
import com.example.kxbackend.domain.generate.repository.OpenAiImageGenerateJobOptionRepository;
import com.example.kxbackend.domain.generate.repository.OpenAiImageGenerateJobRepository;
import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.media.entity.enums.MediaType;
import com.example.kxbackend.domain.media.repository.MediaFileRepository;
import com.example.kxbackend.domain.user.entity.User;
import com.example.kxbackend.domain.user.repository.UserRepository;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import com.example.kxbackend.infra.ai.openai.OpenAiBatchClient;
import com.example.kxbackend.infra.ai.openai.OpenAiImageBatchResultClient;
import com.example.kxbackend.infra.ai.openai.OpenAiImageBatchResultClient.OpenAiBatchStatusResponse;
import com.example.kxbackend.infra.storage.OpenAiGeneratedImageStorageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * OpenAI 배치 API 기반 이미지 생성 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OpenAiGenerateImageService {

    private static final List<Status> PENDING_IMAGE_JOB_STATUSES = List.of(Status.SUBMITTED, Status.IN_PROGRESS);

    private final OpenAiBatchClient openAiBatchClient;
    private final OpenAiImageBatchResultClient openAiImageBatchResultClient;
    private final OpenAiImageGenerateJobRepository openAiImageGenerateJobRepository;
    private final OpenAiImageGenerateJobOptionRepository openAiImageGenerateJobOptionRepository;
    private final MediaFileRepository mediaFileRepository;
    private final UserRepository userRepository;
    private final OpenAiGeneratedImageStorageService openAiGeneratedImageStorageService;
    private final CharacterConceptArtPromptBuilder characterConceptArtPromptBuilder;
    private final ObjectMapper objectMapper;

    @Value("${openai.image.model:gpt-image-2}")
    private String imageModel;

    @Value("${openai.image.size:1024x1024}")
    private String defaultImageSize;

    @Value("${openai.image.character-sheet.size:1536x1024}")
    private String defaultCharacterSheetSize;

    @Value("${openai.image.quality:high}")
    private String defaultImageQuality;

    /**
     * OpenAI 배치 API로 이미지 생성을 요청한다.
     */
    @Transactional
    public OpenAiGenerateImageJobResponseDto requestImageGeneration(Long userId, OpenAiGenerateImageRequestDto request) {
        return submitImageGenerationJob(
                userId,
                request.prompt(),
                resolveImageCount(request.imageCount()),
                resolveSize(request.size()),
                resolveQuality(request.quality())
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
                resolveImageCount(request.imageCount()),
                resolveCharacterSheetSize(request.size()),
                resolveQuality(request.quality())
        );
    }

    private OpenAiGenerateImageJobResponseDto submitImageGenerationJob(
            Long userId,
            String prompt,
            int imageCount,
            String size,
            String quality
    ) {
        User user = getUser(userId);

        GenerateJob imageJob = GenerateJob.builder()
                .user(user)
                .type(Type.TEXT_TO_IMAGE)
                .status(Status.CREATED)
                .build();
        imageJob.addPrompt(PromptKind.IMAGE, 1, prompt);
        openAiImageGenerateJobRepository.save(imageJob);

        OpenAiImageGenerateJobOption jobOption = OpenAiImageGenerateJobOption.of(imageJob, imageCount, size, quality);
        openAiImageGenerateJobOptionRepository.save(jobOption);

        String jsonlContent = convertPromptToJsonl(prompt, imageCount, size, quality);
        String fileName = "openai-image-batch-" + imageJob.getId() + ".jsonl";
        String fileId = openAiBatchClient.uploadBatchFile(jsonlContent, fileName);
        String batchId = openAiBatchClient.submitBatchJob(fileId);

        imageJob.submit(batchId);
        log.info("OpenAI 이미지 생성 배치 제출 완료. jobId={}, batchId={}, imageCount={}",
                imageJob.getId(), batchId, imageCount);

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
            try {
                processPendingImageGenerationJob(imageJob);
            } catch (Exception exception) {
                log.error("OpenAI 이미지 생성 작업 처리 실패. jobId={}, batchId={}",
                        imageJob.getId(), imageJob.getFalRequestId(), exception);
                imageJob.fail("OpenAI 이미지 생성 작업 처리 중 오류가 발생했습니다.");
            }
        }
    }

    private void processPendingImageGenerationJob(GenerateJob imageJob) {
        OpenAiBatchStatusResponse statusResponse = openAiImageBatchResultClient
                .parseBatchStatus(openAiBatchClient.checkBatchStatus(imageJob.getFalRequestId()));
        String status = statusResponse.status();

        if ("completed".equalsIgnoreCase(status)) {
            completeImageGenerationJob(imageJob, statusResponse.outputFileId());
            return;
        }

        if ("failed".equalsIgnoreCase(status) || "cancelled".equalsIgnoreCase(status) || "expired".equalsIgnoreCase(status)) {
            imageJob.fail("OpenAI 이미지 생성 작업이 실패했습니다. status=" + status);
            return;
        }

        imageJob.progress();
    }

    private void completeImageGenerationJob(GenerateJob imageJob, String outputFileId) {
        if (outputFileId == null || outputFileId.isBlank()) {
            imageJob.fail("완료된 작업의 결과 파일 ID가 없습니다.");
            return;
        }

        String jsonlContent = openAiImageBatchResultClient.downloadFileContent(outputFileId);
        List<byte[]> imageBytesList = openAiImageBatchResultClient.extractAllImageBytes(jsonlContent);

        GeneratePrompt prompt = imageJob.getPrompts().getFirst();
        List<MediaFile> savedMediaFiles = new ArrayList<>();

        for (byte[] imageBytes : imageBytesList) {
            String savedPath = openAiGeneratedImageStorageService.saveGeneratedImage(imageJob.getUser().getId(), imageBytes);

            MediaFile mediaFile = MediaFile.builder()
                    .user(imageJob.getUser())
                    .type(MediaType.IMAGE)
                    .filePath(savedPath)
                    .build();
            mediaFile.connectGeneration(imageJob, prompt);
            savedMediaFiles.add(mediaFileRepository.save(mediaFile));
        }

        imageJob.completeJob(savedMediaFiles.getFirst());
        log.info("OpenAI 이미지 생성 완료. jobId={}, imageCount={}", imageJob.getId(), savedMediaFiles.size());
    }

    private OpenAiGenerateImageJobResponseDto buildResponse(GenerateJob imageJob, OpenAiImageGenerateJobOption jobOption) {
        List<MediaFile> resultMediaFiles = mediaFileRepository.findAllByGenerateJob_IdOrderByIdAsc(imageJob.getId());
        return OpenAiGenerateImageJobResponseDto.from(imageJob, jobOption, resultMediaFiles);
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

    private String convertPromptToJsonl(String prompt, int imageCount, String size, String quality) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", imageModel);
            body.put("prompt", prompt);
            body.put("n", imageCount);
            body.put("size", size);
            body.put("quality", quality);

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
}
