package com.example.kxbackend.domain.media.controller;

import com.example.kxbackend.domain.media.service.MediaImageDownloadService;
import com.example.kxbackend.global.security.UserPrincipal;
import com.example.kxbackend.infra.storage.MediaImageDownloadStorageService.DownloadedMediaFile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 이미지 다운로드 API
 */
@RestController
@RequestMapping("/api/media/images")
@RequiredArgsConstructor
public class MediaImageDownloadController {

    private final MediaImageDownloadService mediaImageDownloadService;

    /**
     * 업로드된 이미지 파일을 다운로드한다.
     */
    @GetMapping("/{mediaFileId}/download")
    public ResponseEntity<byte[]> downloadImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long mediaFileId
    ) {
        DownloadedMediaFile downloadedMediaFile =
                mediaImageDownloadService.downloadImage(principal.getId(), mediaFileId);

        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(downloadedMediaFile.fileName())
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.parseMediaType(downloadedMediaFile.contentType()))
                .body(downloadedMediaFile.content());
    }
}
