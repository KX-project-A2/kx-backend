package com.example.kxbackend.domain.auth.controller;

import com.example.kxbackend.domain.auth.dto.request.EmailCheckRequestDto;
import com.example.kxbackend.domain.auth.dto.request.LoginRequestDto;
import com.example.kxbackend.domain.auth.dto.request.PasswordForgotRequestDto;
import com.example.kxbackend.domain.auth.dto.request.PasswordResetRequestDto;
import com.example.kxbackend.domain.auth.dto.request.SignUpRequestDto;
import com.example.kxbackend.domain.auth.dto.response.EmailCheckResponseDto;
import com.example.kxbackend.domain.auth.dto.response.LoginResponseDto;
import com.example.kxbackend.domain.auth.dto.response.SignUpResponseDto;
import com.example.kxbackend.domain.auth.dto.response.TokenResponseDto;
import com.example.kxbackend.domain.auth.service.AuthenticatedUserResult;
import com.example.kxbackend.domain.auth.service.AuthService;
import com.example.kxbackend.domain.auth.service.PasswordResetService;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import com.example.kxbackend.global.response.ApiResponse;
import com.example.kxbackend.global.security.AuthCookieService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 관련 API
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final AuthCookieService authCookieService;

    /**
     * 이메일 중복 확인
     * - 회원가입 전 입력한 이메일을 사용할 수 있는지 확인한다.
     */
    @GetMapping("/email/check")
    public ApiResponse<EmailCheckResponseDto> checkEmail(
            @Valid @ModelAttribute EmailCheckRequestDto request
    ) {
        EmailCheckResponseDto result = authService.checkEmail(request.email());
        return ApiResponse.success("이메일 중복 확인이 완료되었습니다.", result);
    }

    /**
     * 회원가입
     * - 이메일, 비밀번호, 닉네임으로 계정을 생성하고 JWT 토큰을 발급한다.
     */
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SignUpResponseDto> signUp(
            @Valid @RequestBody SignUpRequestDto request,
            HttpServletResponse response
    ) {
        AuthenticatedUserResult result = authService.signUp(request);
        authCookieService.addTokenCookies(response, result.token());
        return ApiResponse.success("회원가입이 완료되었습니다.", SignUpResponseDto.of(result.user()));
    }

    /**
     * 로그인
     * - 이메일, 비밀번호로 인증 후 JWT 토큰을 발급한다.
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponseDto> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletResponse response
    ) {
        AuthenticatedUserResult result = authService.login(request);
        authCookieService.addTokenCookies(response, result.token());
        return ApiResponse.success("로그인에 성공했습니다.", LoginResponseDto.of(result.user()));
    }

    /**
     * 토큰 재발급
     * - refresh token 검증 후 새 JWT 토큰을 발급한다.
     */
    @PostMapping("/reissue")
    public ApiResponse<Void> reissue(
            @CookieValue(value = AuthCookieService.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        TokenResponseDto token = authService.reissue(getRequiredRefreshToken(refreshToken));
        authCookieService.addTokenCookies(response, token);
        return ApiResponse.success("토큰이 재발급되었습니다.");
    }

    /**
     * 로그아웃
     * - refresh token을 무효화한다.
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CookieValue(value = AuthCookieService.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        authCookieService.deleteTokenCookies(response);
        return ApiResponse.success("로그아웃되었습니다.");
    }

    /**
     * 비밀번호 재설정 메일 요청
     * - 일반(LOCAL) 계정에만 메일을 보내며, 응답 메시지는 항상 동일하다.
     */
    @PostMapping("/password/forgot")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody PasswordForgotRequestDto request) {
        passwordResetService.requestPasswordReset(request);
        return ApiResponse.success("비밀번호 재설정 안내가 이메일로 발송되었습니다. 가입된 이메일이 아니면 메일이 전송되지 않습니다.");
    }

    /**
     * 비밀번호 재설정 토큰 유효성 확인
     */
    @GetMapping("/password/reset/validate")
    public ApiResponse<Void> validatePasswordResetToken(@RequestParam("token") String token) {
        passwordResetService.validateResetToken(token);
        return ApiResponse.success("유효한 비밀번호 재설정 토큰입니다.");
    }

    /**
     * 비밀번호 재설정 실행
     */
    @PostMapping("/password/reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody PasswordResetRequestDto request) {
        passwordResetService.resetPassword(request);
        return ApiResponse.success("비밀번호가 재설정되었습니다.");
    }

    private String getRequiredRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        return refreshToken;
    }
}
