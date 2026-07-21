package com.example.kxbackend.domain.share.controller;

import com.example.kxbackend.domain.share.dto.response.ShareLinkResponseDto;
import com.example.kxbackend.domain.share.dto.response.SharedMediaResponseDto;
import com.example.kxbackend.domain.share.service.ShareLinkService;
import com.example.kxbackend.global.response.ApiResponse;
import com.example.kxbackend.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ShareLinkController {

    private final ShareLinkService shareLinkService;

    @PostMapping("/api/media/files/{mediaFileId}/share-links")
    public ApiResponse<ShareLinkResponseDto> createShareLink(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long mediaFileId
    ) {
        ShareLinkResponseDto response = shareLinkService.createShareLink(principal.getId(), mediaFileId);
        return ApiResponse.success("공유 링크가 생성되었습니다.", response);
    }

    @GetMapping("/api/share/{shareToken}")
    public ApiResponse<SharedMediaResponseDto> getSharedMedia(
            @PathVariable String shareToken
    ) {
        SharedMediaResponseDto response = shareLinkService.getSharedMedia(shareToken);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/api/media/files/{mediaFileId}/share-links/{shareLinkId}")
    public ApiResponse<Void> revokeShareLink(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long mediaFileId,
            @PathVariable Long shareLinkId
    ) {
        shareLinkService.revokeShareLink(principal.getId(), mediaFileId, shareLinkId);
        return ApiResponse.success("공유 링크가 비활성화되었습니다.", null);
    }
}
