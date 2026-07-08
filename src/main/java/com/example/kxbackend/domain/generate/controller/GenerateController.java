package com.example.kxbackend.domain.generate.controller;

import com.example.kxbackend.domain.generate.dto.request.FalWebhookRequestDto;
import com.example.kxbackend.domain.generate.dto.request.ImageToVideoGenerateRequestDto;
import com.example.kxbackend.domain.generate.dto.response.GenerateJobResponseDto;
import com.example.kxbackend.domain.generate.entity.GenerateJob;
import com.example.kxbackend.domain.generate.service.GenerateService;
import com.example.kxbackend.domain.user.entity.User;
import com.example.kxbackend.domain.user.repository.UserRepository;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import com.example.kxbackend.global.response.ApiResponse;
import com.example.kxbackend.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/generate")
@RequiredArgsConstructor
public class GenerateController {

    private final GenerateService generateService;
    private final UserRepository userRepository;

    /**
     * 이미지 기반 영상 생성 작업 요청
     */
    @PostMapping("/videos")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GenerateJobResponseDto> createImageToVideoJob(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ImageToVideoGenerateRequestDto request
    ) {
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        GenerateJob generateJob = generateService.createImageToVideoJob(user, request);
        return ApiResponse.success("영상 생성 작업이 요청되었습니다.", GenerateJobResponseDto.from(generateJob));
    }

    /**
     * fal.ai 생성 완료 webhook 처리
     */
    @PostMapping("/webhooks/fal")
    public ApiResponse<GenerateJobResponseDto> handleFalWebhook(@RequestBody FalWebhookRequestDto request) {
        GenerateJob generateJob = generateService.handleFalWebhook(request);
        return ApiResponse.success("fal.ai webhook이 처리되었습니다.", GenerateJobResponseDto.from(generateJob));
    }
}
