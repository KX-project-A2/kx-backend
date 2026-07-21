package com.example.kxbackend.domain.media.dto.response;

import com.example.kxbackend.domain.generate.entity.GenerateJob;
import com.example.kxbackend.domain.generate.entity.enums.Status;
import com.example.kxbackend.domain.generate.entity.enums.Type;

import java.time.LocalDateTime;
import java.util.List;

public record RecentMediaWorkResponseDto(
        Long generateJobId,
        Type type,
        Status status,
        String prompt,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        List<RecentMediaWorkItemResponseDto> items
) {

    public static RecentMediaWorkResponseDto of(
            GenerateJob generateJob,
            String prompt,
            List<RecentMediaWorkItemResponseDto> items
    ) {
        return new RecentMediaWorkResponseDto(
                generateJob.getId(),
                generateJob.getType(),
                generateJob.getStatus(),
                prompt,
                generateJob.getCreatedAt(),
                generateJob.getCompletedAt(),
                items
        );
    }
}
