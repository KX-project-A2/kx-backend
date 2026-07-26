package com.example.kxbackend.domain.generate.controller;

import com.example.kxbackend.domain.generate.dto.request.ReversePromptGenerateRequestDto;
import com.example.kxbackend.domain.generate.dto.request.ReversePromptRequestDto;
import com.example.kxbackend.domain.generate.dto.request.ReversePromptUpdateRequestDto;
import com.example.kxbackend.domain.generate.dto.response.OpenAiGenerateImageJobResponseDto;
import com.example.kxbackend.domain.generate.dto.response.ReversePromptResponseDto;
import com.example.kxbackend.domain.generate.service.ReversePromptService;
import com.example.kxbackend.global.response.ApiResponse;
import com.example.kxbackend.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 역프롬프트 추출/편집/재생성 API
 */
@RestController
@RequestMapping("/api/generate")
@RequiredArgsConstructor
public class ReversePromptController {

    private final ReversePromptService reversePromptService;

    /**
     * 이미지에서 AI 이미지 생성용 프롬프트를 추출한다. (S001/S002)
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

    /**
     * 저장된 역프롬프트를 조회한다.
     */
    @GetMapping("/reverse-prompt/{reversePromptId}")
    public ApiResponse<ReversePromptResponseDto> getReversePrompt(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long reversePromptId
    ) {
        ReversePromptResponseDto response =
                reversePromptService.getReversePrompt(principal.getId(), reversePromptId);
        return ApiResponse.success(response);
    }

    /**
     * 추출된 역프롬프트를 수정한다. (S003)
     */
    @PatchMapping("/reverse-prompt/{reversePromptId}")
    public ApiResponse<ReversePromptResponseDto> updateReversePrompt(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long reversePromptId,
            @Valid @RequestBody ReversePromptUpdateRequestDto request
    ) {
        ReversePromptResponseDto response = reversePromptService.updateReversePrompt(
                principal.getId(),
                reversePromptId,
                request
        );
        return ApiResponse.success("역프롬프트가 수정되었습니다.", response);
    }

    /**
     * 저장된 역프롬프트로 이미지를 재생성한다. (S004)
     */
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping("/reverse-prompt/{reversePromptId}/generate")
    public ApiResponse<OpenAiGenerateImageJobResponseDto> generateFromReversePrompt(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long reversePromptId,
            @Valid @RequestBody ReversePromptGenerateRequestDto request
    ) {
        OpenAiGenerateImageJobResponseDto response = reversePromptService.generateFromReversePrompt(
                principal.getId(),
                reversePromptId,
                request
        );
        return ApiResponse.success("역프롬프트 기반 이미지 생성 요청이 접수되었습니다.", response);
    }
}
