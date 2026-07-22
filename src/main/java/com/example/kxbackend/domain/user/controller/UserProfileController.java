package com.example.kxbackend.domain.user.controller;

import com.example.kxbackend.domain.user.dto.request.ProfileUpdateRequestDto;
import com.example.kxbackend.domain.user.dto.response.GenerationSummaryResponseDto;
import com.example.kxbackend.domain.user.dto.response.ProfileResponseDto;
import com.example.kxbackend.domain.user.service.UserGenerationSummaryService;
import com.example.kxbackend.domain.user.service.UserProfileService;
import com.example.kxbackend.global.response.ApiResponse;
import com.example.kxbackend.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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

    @PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProfileResponseDto> uploadProfileImage(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestPart("file") MultipartFile file
    ) {
        ProfileResponseDto response = userProfileService.uploadProfileImage(principal.getId(), file);
        return ApiResponse.success("프로필 이미지가 업로드되었습니다.", response);
    }

    @DeleteMapping("/profile-image")
    public ApiResponse<ProfileResponseDto> deleteProfileImage(@AuthenticationPrincipal UserPrincipal principal) {
        ProfileResponseDto response = userProfileService.deleteProfileImage(principal.getId());
        return ApiResponse.success("프로필 이미지가 삭제되었습니다.", response);
    }

    @GetMapping("/generation-summary")
    public ApiResponse<GenerationSummaryResponseDto> getGenerationSummary(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.success(userGenerationSummaryService.getSummary(principal.getId()));
    }
}
