package com.example.kxbackend.domain.auth.service;

import com.example.kxbackend.domain.auth.dto.request.LoginRequestDto;
import com.example.kxbackend.domain.auth.dto.request.LogoutRequestDto;
import com.example.kxbackend.domain.auth.dto.request.SignUpRequestDto;
import com.example.kxbackend.domain.auth.dto.response.LoginResponseDto;
import com.example.kxbackend.domain.auth.dto.response.SignUpResponseDto;
import com.example.kxbackend.domain.auth.dto.response.TokenResponseDto;
import com.example.kxbackend.domain.user.entity.User;
import com.example.kxbackend.domain.user.entity.enums.Role;
import com.example.kxbackend.domain.user.repository.UserRepository;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import com.example.kxbackend.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 회원가입
     * 1. 이메일 중복 검사
     * 2. 비밀번호 BCrypt 암호화 후 사용자 저장
     * 3. JWT access/refresh token 발급
     */
    @Transactional
    public SignUpResponseDto signUp(SignUpRequestDto request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);
        TokenResponseDto token = issueTokens(savedUser);
        return SignUpResponseDto.of(savedUser, token);
    }

    /**
     * 로그인
     * 1. 이메일로 사용자 조회
     * 2. 비밀번호 일치 여부 검증
     * 3. JWT access/refresh token 발급
     */
    @Transactional
    public LoginResponseDto login(LoginRequestDto request) {
        User user = userRepository.findByEmail(request.email())
                .filter(foundUser -> passwordEncoder.matches(request.password(), foundUser.getPassword()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        TokenResponseDto token = issueTokens(user);
        return LoginResponseDto.of(user, token);
    }

    /**
     * 로그아웃
     * - DB에 저장된 refresh token을 삭제하여 재발급을 무효화한다.
     */
    @Transactional
    public void logout(LogoutRequestDto request) {
        refreshTokenService.deleteByToken(request.refreshToken());
    }

    /**
     * JWT 토큰 발급
     * - access token: 사용자 ID와 역할(Role)을 claim에 포함
     * - refresh token: DB에 저장하여 재발급 및 로그아웃 시 무효화에 활용
     */
    private TokenResponseDto issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        refreshTokenService.save(user, refreshToken);
        return new TokenResponseDto(accessToken, refreshToken);
    }
}
