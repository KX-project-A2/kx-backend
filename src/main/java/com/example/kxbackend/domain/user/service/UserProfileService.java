package com.example.kxbackend.domain.user.service;

import com.example.kxbackend.domain.user.dto.request.ProfileUpdateRequestDto;
import com.example.kxbackend.domain.user.dto.response.ProfileResponseDto;
import com.example.kxbackend.domain.user.entity.User;
import com.example.kxbackend.domain.user.repository.UserRepository;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import com.example.kxbackend.infra.storage.ObjectStorageKeys;
import com.example.kxbackend.infra.storage.s3.S3PresignedUrlService;
import com.example.kxbackend.infra.storage.service.ImageUploadStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserRepository userRepository;
    private final ImageUploadStorageService imageUploadStorageService;
    private final S3PresignedUrlService s3PresignedUrlService;

    public ProfileResponseDto getProfile(Long userId) {
        return toProfileResponse(getUser(userId));
    }

    @Transactional
    public ProfileResponseDto updateProfile(Long userId, ProfileUpdateRequestDto request) {
        User user = getUser(userId);
        user.updateNickname(request.nickname());
        return toProfileResponse(user);
    }

    @Transactional
    public ProfileResponseDto uploadProfileImage(Long userId, MultipartFile file) {
        User user = getUser(userId);
        String oldProfileImagePath = user.getProfileImagePath();
        String newProfileImagePath = imageUploadStorageService.uploadProfileImage(userId, file);

        user.updateProfileImagePath(newProfileImagePath);
        deleteOldProfileImageIfExists(oldProfileImagePath);
        return toProfileResponse(user);
    }

    @Transactional
    public ProfileResponseDto deleteProfileImage(Long userId) {
        User user = getUser(userId);
        String profileImagePath = user.getProfileImagePath();
        if (!StringUtils.hasText(profileImagePath)) {
            return toProfileResponse(user);
        }

        imageUploadStorageService.delete(profileImagePath);
        user.deleteProfileImage();
        return toProfileResponse(user);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private ProfileResponseDto toProfileResponse(User user) {
        return ProfileResponseDto.from(user, createProfileImageUrl(user.getProfileImagePath()));
    }

    private String createProfileImageUrl(String profileImagePath) {
        if (!StringUtils.hasText(profileImagePath)) {
            return null;
        }

        String objectKey = ObjectStorageKeys.normalize(profileImagePath);
        return s3PresignedUrlService.createReadUrl(objectKey).url();
    }

    private void deleteOldProfileImageIfExists(String oldProfileImagePath) {
        if (!StringUtils.hasText(oldProfileImagePath)) {
            return;
        }

        try {
            imageUploadStorageService.delete(oldProfileImagePath);
        } catch (BusinessException exception) {
            log.warn("이전 프로필 이미지 삭제 실패. objectKey={}", oldProfileImagePath, exception);
        }
    }
}
