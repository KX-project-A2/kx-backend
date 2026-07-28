package com.example.kxbackend.domain.generate.controller;

import com.example.kxbackend.domain.generate.dto.request.CharacterConceptSheetRequestDto;
import com.example.kxbackend.domain.generate.dto.request.OpenAiGenerateImageRequestDto;
import com.example.kxbackend.domain.generate.dto.response.OpenAiActiveImageJobResponseDto;
import com.example.kxbackend.domain.generate.dto.response.OpenAiGenerateImageJobResponseDto;
import com.example.kxbackend.domain.generate.service.OpenAiGenerateImageService;
import com.example.kxbackend.global.response.ApiResponse;
import com.example.kxbackend.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * OpenAI 이미지 생성 API
 */
@RestController
@RequestMapping("/api/generate/images")
@RequiredArgsConstructor
public class OpenAiGenerateImageController {

    private final OpenAiGenerateImageService openAiGenerateImageService;

    /**
     * 이미지 생성을 요청한다. 레퍼런스 이미지는 0~8장까지 첨부할 수 있다.
     */
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<OpenAiGenerateImageJobResponseDto> requestImageGeneration(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestPart("request") OpenAiGenerateImageRequestDto request,
            @RequestPart(value = "references", required = false) List<MultipartFile> references
    ) {
        OpenAiGenerateImageJobResponseDto response =
                openAiGenerateImageService.requestImageGeneration(
                        principal.getId(),
                        request,
                        references
                );
        return ApiResponse.success("OpenAI 이미지 생성 요청이 접수되었습니다.", response);
    }

    /**
     * 구조화된 캐릭터 데이터로 공식 캐릭터 설정표(Concept Art Sheet) 생성을 요청한다.
     * 레퍼런스 이미지는 0~8장까지 첨부할 수 있다.
     */
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(value = "/character-concept-sheet", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<OpenAiGenerateImageJobResponseDto> requestCharacterConceptSheet(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestPart("request") CharacterConceptSheetRequestDto request,
            @RequestPart(value = "references", required = false) List<MultipartFile> references
    ) {
        OpenAiGenerateImageJobResponseDto response =
                openAiGenerateImageService.requestCharacterConceptSheet(
                        principal.getId(),
                        request,
                        references
                );
        return ApiResponse.success("캐릭터 설정표 생성 요청이 접수되었습니다.", response);
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

    /**
     * 진행 중인 이미지 생성 작업 목록을 조회한다.
     */
    @GetMapping("/jobs/active")
    public ApiResponse<List<OpenAiActiveImageJobResponseDto>> getActiveImageJobs(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<OpenAiActiveImageJobResponseDto> response =
                openAiGenerateImageService.getActiveImageJobs(principal.getId());
        return ApiResponse.success(response);
    }
}
