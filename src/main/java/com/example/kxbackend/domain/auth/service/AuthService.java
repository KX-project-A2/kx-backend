package com.example.kxbackend.domain.auth.service;

import com.example.kxbackend.domain.auth.dto.request.LoginRequestDto;
import com.example.kxbackend.domain.auth.dto.request.SignUpRequestDto;
import com.example.kxbackend.domain.auth.dto.response.EmailCheckResponseDto;
import com.example.kxbackend.domain.auth.entity.RefreshToken;
import com.example.kxbackend.domain.auth.dto.response.TokenResponseDto;
import com.example.kxbackend.domain.user.entity.User;
import com.example.kxbackend.domain.user.entity.enums.AuthProvider;
import com.example.kxbackend.domain.user.entity.enums.Role;
import com.example.kxbackend.domain.user.repository.UserRepository;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 비즈니스 로직
 * - 회원가입, 로그인, 로그아웃, 토큰 발급 등 인증 관련 처리를 담당한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final AuthTokenService authTokenService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 이메일 중복 확인
     */
    public EmailCheckResponseDto checkEmail(String email) {
        boolean duplicated = userRepository.existsByEmail(email);
        return EmailCheckResponseDto.of(email, duplicated);
    }


    /**
     * 회원가입
     * 1. 이메일 중복 검사
     * 2. 비밀번호 BCrypt 암호화 후 사용자 저장
     * 3. JWT access/refresh token 발급
     */
    @Transactional
    public AuthenticatedUserResult signUp(SignUpRequestDto request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .provider(AuthProvider.LOCAL)
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);
        TokenResponseDto token = authTokenService.issueTokens(savedUser);
        return new AuthenticatedUserResult(savedUser, token);
    }

    /**
     * 로그인
     * 1. 이메일로 사용자 조회
     * 2. 비밀번호 일치 여부 검증
     * 3. JWT access/refresh token 발급
     */
    @Transactional
    public AuthenticatedUserResult login(LoginRequestDto request) {
        User user = userRepository.findByEmail(request.email())
                .filter(foundUser -> foundUser.getProvider() == AuthProvider.LOCAL)
                .filter(foundUser -> foundUser.getPassword() != null)
                .filter(foundUser -> passwordEncoder.matches(request.password(), foundUser.getPassword()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        TokenResponseDto token = authTokenService.issueTokens(user);
        return new AuthenticatedUserResult(user, token);
    }

    /**
     * 토큰 재발급
     * 1. refresh token 유효성 검증
     * 2. 새 access/refresh token 발급
     * 3. refresh token rotation
     */
    @Transactional
    public TokenResponseDto reissue(String refreshToken) {
        RefreshToken savedToken = refreshTokenService.findValidToken(refreshToken);
        User user = savedToken.getUser();

        return authTokenService.rotateTokens(refreshToken, user);
    }

    /**
     * 로그아웃
     * - DB에 저장된 refresh token을 삭제하여 재발급을 무효화한다.
     */
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.deleteByToken(refreshToken);
    }
}
