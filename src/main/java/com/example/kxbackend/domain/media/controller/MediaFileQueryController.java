package com.example.kxbackend.domain.media.controller;

import com.example.kxbackend.domain.media.dto.response.MediaFileDownloadUrlResponseDto;
import com.example.kxbackend.domain.media.dto.response.MediaFilePageResponseDto;
import com.example.kxbackend.domain.media.dto.response.MediaFileResponseDto;
import com.example.kxbackend.domain.media.dto.response.RecentMediaWorkResponseDto;
import com.example.kxbackend.domain.media.entity.enums.MediaType;
import com.example.kxbackend.domain.media.service.MediaFileDownloadUrlService;
import com.example.kxbackend.domain.media.service.MediaFileQueryService;
import com.example.kxbackend.global.response.ApiResponse;
import com.example.kxbackend.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/media/files")
@RequiredArgsConstructor
public class MediaFileQueryController {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final MediaFileQueryService mediaFileQueryService;
    private final MediaFileDownloadUrlService mediaFileDownloadUrlService;

    @GetMapping
    public ApiResponse<MediaFilePageResponseDto> getMediaFiles(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) MediaType type,
            @RequestParam(required = false) Boolean favorite,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = createPageable(page, size);
        MediaFilePageResponseDto response =
                mediaFileQueryService.getMediaFiles(principal.getId(), type, favorite, pageable);
        return ApiResponse.success(response);
    }

    @GetMapping("/recent")
    public ApiResponse<List<RecentMediaWorkResponseDto>> getRecentMediaWorks(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "6") int size
    ) {
        List<RecentMediaWorkResponseDto> response =
                mediaFileQueryService.getRecentMediaWorks(principal.getId(), size);
        return ApiResponse.success(response);
    }

    @GetMapping("/{mediaFileId}")
    public ApiResponse<MediaFileResponseDto> getMediaFile(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long mediaFileId
    ) {
        MediaFileResponseDto response = mediaFileQueryService.getMediaFile(principal.getId(), mediaFileId);
        return ApiResponse.success(response);
    }

    @GetMapping("/{mediaFileId}/download-url")
    public ApiResponse<MediaFileDownloadUrlResponseDto> getMediaFileDownloadUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long mediaFileId
    ) {
        MediaFileDownloadUrlResponseDto response =
                mediaFileDownloadUrlService.createDownloadUrl(principal.getId(), mediaFileId);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{mediaFileId}")
    public ApiResponse<Void> deleteMediaFile(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long mediaFileId
    ) {
        mediaFileQueryService.deleteMediaFile(principal.getId(), mediaFileId);
        return ApiResponse.success("작업물이 삭제되었습니다.", null);
    }

    private Pageable createPageable(int page, int size) {
        int resolvedPage = Math.max(page, DEFAULT_PAGE);
        int resolvedSize = Math.min(Math.max(size, 1), MAX_SIZE);
        return PageRequest.of(resolvedPage, resolvedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
