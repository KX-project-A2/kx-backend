package com.example.kxbackend.domain.user.service;

import com.example.kxbackend.domain.user.dto.request.ProfileUpdateRequestDto;
import com.example.kxbackend.domain.user.dto.response.ProfileResponseDto;
import com.example.kxbackend.domain.user.entity.User;
import com.example.kxbackend.domain.user.repository.UserRepository;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileService {

    private final UserRepository userRepository;

    public ProfileResponseDto getProfile(Long userId) {
        return ProfileResponseDto.from(getUser(userId));
    }

    @Transactional
    public ProfileResponseDto updateProfile(Long userId, ProfileUpdateRequestDto request) {
        User user = getUser(userId);
        user.updateNickname(request.nickname());
        return ProfileResponseDto.from(user);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
