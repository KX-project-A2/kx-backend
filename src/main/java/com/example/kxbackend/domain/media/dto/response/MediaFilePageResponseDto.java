package com.example.kxbackend.domain.media.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record MediaFilePageResponseDto(
        List<MediaFileResponseDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {

    public static MediaFilePageResponseDto from(Page<MediaFileResponseDto> page) {
        return new MediaFilePageResponseDto(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
