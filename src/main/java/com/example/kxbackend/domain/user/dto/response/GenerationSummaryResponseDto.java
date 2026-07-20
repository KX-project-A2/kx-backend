package com.example.kxbackend.domain.user.dto.response;

import java.time.LocalDateTime;

public record GenerationSummaryResponseDto(
        long totalMediaCount,
        long imageCount,
        long videoCount,
        LocalDateTime latestGeneratedAt
) {
}
