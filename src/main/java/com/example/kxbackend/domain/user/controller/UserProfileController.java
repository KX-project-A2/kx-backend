package com.example.kxbackend.domain.user.controller;

import com.example.kxbackend.domain.user.dto.request.ProfileUpdateRequestDto;
import com.example.kxbackend.domain.user.dto.response.GenerationSummaryResponseDto;
import com.example.kxbackend.domain.user.dto.response.ProfileResponseDto;
import com.example.kxbackend.domain.user.service.UserGenerationSummaryService;
import com.example.kxbackend.domain.user.service.UserProfileService;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import com.example.kxbackend.global.response.ApiResponse;
import com.example.kxbackend.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestPart;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final UserGenerationSummaryService userGenerationSummaryService;

    @GetMapping("/profile")
    public ApiResponse<ProfileResponseDto> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(userProfileService.getProfile(principal.getId()));
    }

    @PatchMapping("/profile")
    public ApiResponse<ProfileResponseDto> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ProfileUpdateRequestDto request
    ) {
        ProfileResponseDto response = userProfileService.updateProfile(principal.getId(), request);
        return ApiResponse.success("프로필이 수정되었습니다.", response);
    }

    @PostMapping("/profile-image")
    public ApiResponse<Void> uploadProfileImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart("file") MultipartFile file
    ) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED, "프로필 이미지 업로드는 S3 저장소 연동 후 지원됩니다.");
    }

    @DeleteMapping("/profile-image")
    public ApiResponse<Void> deleteProfileImage(@AuthenticationPrincipal UserPrincipal principal) {
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED, "프로필 이미지 삭제는 S3 저장소 연동 후 지원됩니다.");
    }

    @GetMapping("/generation-summary")
    public ApiResponse<GenerationSummaryResponseDto> getGenerationSummary(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.success(userGenerationSummaryService.getSummary(principal.getId()));
    }
}
