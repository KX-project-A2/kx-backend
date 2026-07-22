package com.example.kxbackend.domain.generate.controller;

import com.example.kxbackend.domain.generate.dto.request.ReversePromptRequestDto;
import com.example.kxbackend.domain.generate.dto.response.ReversePromptResponseDto;
import com.example.kxbackend.domain.generate.service.ReversePromptService;
import com.example.kxbackend.global.response.ApiResponse;
import com.example.kxbackend.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 역프롬프트 추출 API
 */
@RestController
@RequestMapping("/api/generate")
@RequiredArgsConstructor
public class ReversePromptController {

    private final ReversePromptService reversePromptService;

    /**
     * 이미지에서 AI 이미지 생성용 프롬프트를 추출한다.
     */
    @PostMapping(value = "/reverse-prompt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ReversePromptResponseDto> extractReversePrompt(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestPart("request") ReversePromptRequestDto request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        ReversePromptResponseDto response = reversePromptService.extractReversePrompt(
                principal.getId(),
                request,
                image
        );
        return ApiResponse.success("역프롬프트 추출이 완료되었습니다.", response);
    }
}
