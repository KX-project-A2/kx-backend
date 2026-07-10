package com.example.kxbackend.domain.media.controller;

import com.example.kxbackend.domain.media.dto.response.MediaImageUploadResponseDto;
import com.example.kxbackend.domain.media.service.MediaImageUploadService;
import com.example.kxbackend.global.response.ApiResponse;
import com.example.kxbackend.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 업로드 API
 */
@RestController
@RequestMapping("/api/media/images/upload")
@RequiredArgsConstructor
public class MediaImageUploadController {

    private final MediaImageUploadService mediaImageUploadService;

    /**
     * 이미지 파일을 업로드한다.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MediaImageUploadResponseDto> uploadImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "tags", required = false) String tags
    ) {
        MediaImageUploadResponseDto response =
                mediaImageUploadService.uploadImage(principal.getId(), file, tags);
        return ApiResponse.success("이미지 업로드가 완료되었습니다.", response);
    }
}
