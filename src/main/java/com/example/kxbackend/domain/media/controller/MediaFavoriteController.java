package com.example.kxbackend.domain.media.controller;

import com.example.kxbackend.domain.media.service.MediaFavoriteService;
import com.example.kxbackend.global.response.ApiResponse;
import com.example.kxbackend.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/media/files")
@RequiredArgsConstructor
public class MediaFavoriteController {

    private final MediaFavoriteService mediaFavoriteService;

    @PostMapping("/{mediaFileId}/favorite")
    public ApiResponse<Void> addFavorite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long mediaFileId
    ) {
        mediaFavoriteService.addFavorite(principal.getId(), mediaFileId);
        return ApiResponse.success("찜하기가 완료되었습니다.", null);
    }

    @DeleteMapping("/{mediaFileId}/favorite")
    public ApiResponse<Void> removeFavorite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long mediaFileId
    ) {
        mediaFavoriteService.removeFavorite(principal.getId(), mediaFileId);
        return ApiResponse.success("찜하기가 해제되었습니다.", null);
    }
}
