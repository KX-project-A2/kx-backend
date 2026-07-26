package com.example.kxbackend.domain.auth.service;

import com.example.kxbackend.domain.auth.dto.request.PasswordForgotRequestDto;
import com.example.kxbackend.domain.auth.dto.request.PasswordResetRequestDto;
import com.example.kxbackend.domain.auth.entity.PasswordResetToken;
import com.example.kxbackend.domain.auth.repository.PasswordResetTokenRepository;
import com.example.kxbackend.domain.auth.repository.RefreshTokenRepository;
import com.example.kxbackend.domain.user.entity.User;
import com.example.kxbackend.domain.user.entity.enums.AuthProvider;
import com.example.kxbackend.domain.user.repository.UserRepository;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import com.example.kxbackend.infra.mail.SesMailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 비밀번호 재설정 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SesMailSender sesMailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.password-reset.frontend-url:http://localhost:3000/reset-password}")
    private String frontendResetUrl;

    @Value("${app.password-reset.token-expiration-ms:3600000}")
    private long tokenExpirationMs;

    /**
     * 비밀번호 재설정 메일을 요청한다.
     * 계정 존재 여부/로그인 방식과 무관하게 동일한 응답을 유지한다.
     */
    @Transactional
    public void requestPasswordReset(PasswordForgotRequestDto request) {
        userRepository.findByEmail(request.email().trim())
                .filter(user -> user.getProvider() == AuthProvider.LOCAL)
                .filter(user -> user.getPassword() != null && !user.getPassword().isBlank())
                .ifPresent(this::issueAndSendResetMail);
    }

    /**
     * 토큰으로 새 비밀번호를 설정한다.
     */
    @Transactional
    public void resetPassword(PasswordResetRequestDto request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(hashToken(request.token()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN));

        if (resetToken.isUsed()) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        }
        if (resetToken.isExpired()) {
            throw new BusinessException(ErrorCode.EXPIRED_PASSWORD_RESET_TOKEN);
        }

        User user = resetToken.getUser();
        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        }

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        passwordResetTokenRepository.deleteByUser(user);
        refreshTokenRepository.deleteByUser(user);
        log.info("비밀번호 재설정 완료. userId={}", user.getId());
    }

    /**
     * 재설정 토큰 유효성만 확인한다.
     */
    public void validateResetToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(hashToken(token))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN));

        if (resetToken.isUsed()) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_RESET_TOKEN);
        }
        if (resetToken.isExpired()) {
            throw new BusinessException(ErrorCode.EXPIRED_PASSWORD_RESET_TOKEN);
        }
    }

    private void issueAndSendResetMail(User user) {
        passwordResetTokenRepository.deleteByUser(user);

        String rawToken = generateRawToken();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .tokenHash(hashToken(rawToken))
                .expiresAt(LocalDateTime.now().plusSeconds(tokenExpirationMs / 1000))
                .build();
        passwordResetTokenRepository.save(resetToken);

        String resetUrl = UriComponentsBuilder
                .fromUriString(frontendResetUrl)
                .queryParam("token", rawToken)
                .build(true)
                .toUriString();

        sesMailSender.sendPasswordResetMail(user.getEmail(), resetUrl);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "비밀번호 재설정 토큰 해시 생성에 실패했습니다.");
        }
    }
}
