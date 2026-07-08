package com.example.kxbackend.domain.generate.controller;

import com.example.kxbackend.domain.generate.dto.request.OpenAiGenerateImageRequestDto;
import com.example.kxbackend.domain.generate.dto.response.OpenAiGenerateImageJobResponseDto;
import com.example.kxbackend.domain.generate.service.OpenAiGenerateImageService;
import com.example.kxbackend.global.response.ApiResponse;
import com.example.kxbackend.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * OpenAI 이미지 생성 API
 */
@RestController
@RequestMapping("/api/openai/generate/images")
@RequiredArgsConstructor
public class OpenAiGenerateImageController {

    private final OpenAiGenerateImageService openAiGenerateImageService;

    /**
     * OpenAI 배치 API로 이미지 생성을 요청한다.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<OpenAiGenerateImageJobResponseDto> requestImageGeneration(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody OpenAiGenerateImageRequestDto request
    ) {
        OpenAiGenerateImageJobResponseDto response =
                openAiGenerateImageService.requestImageGeneration(principal.getId(), request);
        return ApiResponse.success("OpenAI 이미지 생성 요청이 접수되었습니다.", response);
    }

    /**
     * OpenAI 이미지 생성 작업 상태를 조회한다.
     */
    @GetMapping("/jobs/{jobId}")
    public ApiResponse<OpenAiGenerateImageJobResponseDto> getImageJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long jobId
    ) {
        OpenAiGenerateImageJobResponseDto response =
                openAiGenerateImageService.getImageJob(principal.getId(), jobId);
        return ApiResponse.success(response);
    }
}
