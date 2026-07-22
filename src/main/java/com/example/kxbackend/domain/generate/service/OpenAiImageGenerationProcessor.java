package com.example.kxbackend.domain.generate.service;

import com.example.kxbackend.domain.generate.entity.GenerateJob;
import com.example.kxbackend.domain.generate.entity.GeneratePrompt;
import com.example.kxbackend.domain.generate.entity.OpenAiImageGenerateJobOption;
import com.example.kxbackend.domain.generate.entity.OpenAiImageReference;
import com.example.kxbackend.domain.generate.repository.OpenAiImageGenerateJobRepository;
import com.example.kxbackend.domain.generate.repository.OpenAiImageGenerateJobOptionRepository;
import com.example.kxbackend.domain.generate.repository.OpenAiImageReferenceRepository;
import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.media.entity.enums.MediaType;
import com.example.kxbackend.domain.media.repository.MediaFileRepository;
import com.example.kxbackend.infra.ai.openai.OpenAiGenerateImageClient;
import com.example.kxbackend.infra.ai.openai.OpenAiReferenceImageClient;
import com.example.kxbackend.infra.storage.service.OpenAiGeneratedImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * OpenAI Images API 동기 호출을 백그라운드 스레드에서 처리한다.
 * 요청 스레드는 jobId를 즉시 반환하고, 실제 이미지 생성은 이 컴포넌트가 담당한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiImageGenerationProcessor {

    private final OpenAiGenerateImageClient openAiGenerateImageClient;
    private final OpenAiReferenceImageClient openAiReferenceImageClient;
    private final OpenAiImageGenerateJobRepository openAiImageGenerateJobRepository;
    private final OpenAiImageGenerateJobOptionRepository openAiImageGenerateJobOptionRepository;
    private final OpenAiImageReferenceRepository openAiImageReferenceRepository;
    private final MediaFileRepository mediaFileRepository;
    private final OpenAiGeneratedImageStorageService openAiGeneratedImageStorageService;

    @Value("${openai.image.model:gpt-image-2}")
    private String imageModel;

    /**
     * 백그라운드에서 OpenAI 이미지 생성을 수행하고 결과를 저장한다.
     */
    @Async("imageGenerationExecutor")
    @Transactional
    public void process(Long jobId, String generationPrompt, int imageCount, String size, String openAiQuality) {
        GenerateJob imageJob = openAiImageGenerateJobRepository.findById(jobId).orElse(null);
        if (imageJob == null) {
            log.warn("비동기 이미지 생성 대상 작업을 찾을 수 없습니다. jobId={}", jobId);
            return;
        }

        List<OpenAiImageReference> references =
                openAiImageReferenceRepository.findAllByGenerateJob_IdOrderByReferenceOrderAsc(jobId);
        try {
            List<byte[]> generatedImages = references.isEmpty()
                    ? openAiGenerateImageClient.generate(
                            imageModel, generationPrompt, imageCount, size, openAiQuality
                    )
                    : openAiGenerateImageClient.edit(
                            imageModel,
                            generationPrompt,
                            imageCount,
                            size,
                            openAiQuality,
                            references.stream().map(OpenAiImageReference::getOpenAiFileId).toList()
                    );

            saveGeneratedImageResults(imageJob, generatedImages);
            log.info("OpenAI 이미지 비동기 생성 완료. jobId={}, imageCount={}, referenceCount={}",
                    jobId, generatedImages.size(), references.size());
        } catch (Exception exception) {
            log.error("OpenAI 이미지 비동기 생성 실패. jobId={}", jobId, exception);
            imageJob.fail("OpenAI 이미지 생성에 실패했습니다: " + exception.getMessage());
        } finally {
            deleteOpenAiReferenceFiles(references);
        }
    }

    private void saveGeneratedImageResults(GenerateJob imageJob, List<byte[]> imageBytesList) {
        GeneratePrompt prompt = imageJob.getPrompts().getFirst();
        OpenAiImageGenerateJobOption jobOption =
                openAiImageGenerateJobOptionRepository.findById(imageJob.getId()).orElse(null);
        List<MediaFile> savedMediaFiles = new ArrayList<>();

        for (byte[] imageBytes : imageBytesList) {
            String savedPath = openAiGeneratedImageStorageService.saveGeneratedImage(
                    imageJob.getUser().getId(), imageBytes);

            MediaFile mediaFile = MediaFile.builder()
                    .user(imageJob.getUser())
                    .type(MediaType.IMAGE)
                    .filePath(savedPath)
                    .model(imageModel)
                    .quality(jobOption != null ? jobOption.getQuality() : null)
                    .resolution(jobOption != null ? jobOption.getSize() : null)
                    .tags("openai")
                    .build();
            mediaFile.connectGeneration(imageJob, prompt);
            savedMediaFiles.add(mediaFileRepository.save(mediaFile));
        }

        imageJob.completeJob(savedMediaFiles.getFirst());
    }

    private void deleteOpenAiReferenceFiles(List<OpenAiImageReference> references) {
        references.forEach(reference ->
                openAiReferenceImageClient.deleteReferenceImage(reference.getOpenAiFileId()));
    }
}
