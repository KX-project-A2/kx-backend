package com.example.kxbackend.domain.user.service;

import com.example.kxbackend.domain.media.entity.enums.MediaType;
import com.example.kxbackend.domain.media.repository.MediaFileRepository;
import com.example.kxbackend.domain.user.dto.response.GenerationSummaryResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserGenerationSummaryService {

    private final MediaFileRepository mediaFileRepository;

    public GenerationSummaryResponseDto getSummary(Long userId) {
        return new GenerationSummaryResponseDto(
                mediaFileRepository.countByUserIdAndDeletedFalse(userId),
                mediaFileRepository.countByUserIdAndTypeAndDeletedFalse(userId, MediaType.IMAGE),
                mediaFileRepository.countByUserIdAndTypeAndDeletedFalse(userId, MediaType.VIDEO),
                mediaFileRepository.findLatestCreatedAtByUserId(userId).orElse(null)
        );
    }
}
