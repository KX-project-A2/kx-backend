package com.example.kxbackend.domain.media.service;

import com.example.kxbackend.domain.media.dto.response.MediaFilePageResponseDto;
import com.example.kxbackend.domain.media.dto.response.MediaFileResponseDto;
import com.example.kxbackend.domain.media.dto.response.RecentMediaWorkItemResponseDto;
import com.example.kxbackend.domain.media.dto.response.RecentMediaWorkResponseDto;
import com.example.kxbackend.domain.generate.entity.GenerateJob;
import com.example.kxbackend.domain.generate.entity.GeneratePrompt;
import com.example.kxbackend.domain.generate.entity.enums.Status;
import com.example.kxbackend.domain.generate.repository.GenerateJobRepository;
import com.example.kxbackend.domain.media.entity.MediaFile;
import com.example.kxbackend.domain.media.entity.enums.MediaType;
import com.example.kxbackend.domain.media.repository.MediaFavoriteRepository;
import com.example.kxbackend.domain.media.repository.MediaFileRepository;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaFileQueryService {

    private final MediaFileRepository mediaFileRepository;
    private final MediaFavoriteRepository mediaFavoriteRepository;
    private final GenerateJobRepository generateJobRepository;

    public MediaFilePageResponseDto getMediaFiles(Long userId, MediaType type, Boolean favorite, Pageable pageable) {
        Page<MediaFile> mediaFiles = findMediaFiles(userId, type, favorite, pageable);
        Set<Long> favoriteMediaFileIds = getFavoriteMediaFileIds(userId, mediaFiles.getContent());

        return MediaFilePageResponseDto.from(mediaFiles.map(mediaFile ->
                MediaFileResponseDto.from(mediaFile, favoriteMediaFileIds)
        ));
    }

    public MediaFileResponseDto getMediaFile(Long userId, Long mediaFileId) {
        MediaFile mediaFile = mediaFileRepository.findByIdAndUserId(mediaFileId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "미디어 파일을 찾을 수 없습니다."));

        boolean favorite = mediaFavoriteRepository.existsByUserIdAndMediaFileId(userId, mediaFileId);
        return MediaFileResponseDto.from(mediaFile, favorite);
    }

    public List<RecentMediaWorkResponseDto> getRecentMediaWorks(Long userId, int size) {
        int resolvedSize = Math.min(Math.max(size, 1), 100);
        List<Long> jobIds = generateJobRepository.findRecentJobIdsByUserIdAndStatus(
                userId,
                Status.COMPLETED,
                PageRequest.of(0, resolvedSize)
        );

        if (jobIds.isEmpty()) {
            return List.of();
        }

        Map<Long, GenerateJob> jobsById = generateJobRepository.findAllWithPromptsByIdInOrderByCreatedAtDesc(jobIds)
                .stream()
                .collect(Collectors.toMap(
                        GenerateJob::getId,
                        generateJob -> generateJob,
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        List<MediaFile> mediaFiles = mediaFileRepository.findAllByGenerateJob_IdInOrderByCreatedAtDescIdAsc(jobIds);
        Set<Long> favoriteMediaFileIds = getFavoriteMediaFileIds(userId, mediaFiles);
        Map<Long, List<MediaFile>> mediaFilesByJobId = mediaFiles.stream()
                .filter(mediaFile -> mediaFile.getGenerateJob() != null)
                .collect(Collectors.groupingBy(mediaFile -> mediaFile.getGenerateJob().getId()));

        return jobIds.stream()
                .map(jobsById::get)
                .filter(generateJob -> generateJob != null)
                .map(generateJob -> RecentMediaWorkResponseDto.of(
                        generateJob,
                        getFirstPromptContent(generateJob),
                        getRecentMediaWorkItems(mediaFilesByJobId.getOrDefault(generateJob.getId(), List.of()), favoriteMediaFileIds)
                ))
                .toList();
    }

    private Page<MediaFile> findMediaFiles(Long userId, MediaType type, Boolean favorite, Pageable pageable) {
        if (Boolean.TRUE.equals(favorite)) {
            return type == null
                    ? mediaFavoriteRepository.findFavoriteMediaFilesByUserId(userId, pageable)
                    : mediaFavoriteRepository.findFavoriteMediaFilesByUserIdAndType(userId, type, pageable);
        }

        return type == null
                ? mediaFileRepository.findAllByUserId(userId, pageable)
                : mediaFileRepository.findAllByUserIdAndType(userId, type, pageable);
    }

    private Set<Long> getFavoriteMediaFileIds(Long userId, List<MediaFile> mediaFiles) {
        if (mediaFiles.isEmpty()) {
            return Set.of();
        }

        List<Long> mediaFileIds = mediaFiles.stream()
                .map(MediaFile::getId)
                .toList();

        return mediaFavoriteRepository.findFavoriteMediaFileIds(userId, mediaFileIds)
                .stream()
                .collect(Collectors.toSet());
    }

    private String getFirstPromptContent(GenerateJob generateJob) {
        if (generateJob.getPrompts().isEmpty()) {
            return null;
        }
        return generateJob.getPrompts().stream()
                .min(Comparator.comparingInt(GeneratePrompt::getPromptOrder))
                .map(GeneratePrompt::getContent)
                .orElse(null);
    }

    private List<RecentMediaWorkItemResponseDto> getRecentMediaWorkItems(
            List<MediaFile> mediaFiles,
            Set<Long> favoriteMediaFileIds
    ) {
        if (mediaFiles.isEmpty()) {
            return Collections.emptyList();
        }
        return mediaFiles.stream()
                .sorted(Comparator.comparing(MediaFile::getId))
                .map(mediaFile -> RecentMediaWorkItemResponseDto.from(mediaFile, favoriteMediaFileIds))
                .toList();
    }
}
