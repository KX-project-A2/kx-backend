package com.example.kxbackend.domain.auth.controller;

import com.example.kxbackend.domain.auth.dto.request.LoginRequestDto;
import com.example.kxbackend.domain.auth.dto.request.LogoutRequestDto;
import com.example.kxbackend.domain.auth.dto.request.ReissueRequestDto;
import com.example.kxbackend.domain.auth.dto.request.SignUpRequestDto;
import com.example.kxbackend.domain.auth.dto.response.LoginResponseDto;
import com.example.kxbackend.domain.auth.dto.response.SignUpResponseDto;
import com.example.kxbackend.domain.auth.dto.response.TokenResponseDto;
import com.example.kxbackend.domain.auth.service.AuthService;
import com.example.kxbackend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    /**
     * 회원가입
     * - 이메일, 비밀번호, 닉네임으로 계정을 생성하고 JWT 토큰을 발급한다.
     */
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SignUpResponseDto> signUp(@Valid @RequestBody SignUpRequestDto request) {
        SignUpResponseDto response = authService.signUp(request);
        return ApiResponse.success("회원가입이 완료되었습니다.", response);
    }

    /**
     * 로그인
     * - 이메일, 비밀번호로 인증 후 JWT 토큰을 발급한다.
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        LoginResponseDto response = authService.login(request);
        return ApiResponse.success("로그인에 성공했습니다.", response);
    }

    /**
     * 토큰 재발급
     * - refresh token 검증 후 새 JWT 토큰을 발급한다.
     */
    @PostMapping("/reissue")
    public ApiResponse<TokenResponseDto> reissue(@Valid @RequestBody ReissueRequestDto request) {
        TokenResponseDto response = authService.reissue(request);
        return ApiResponse.success("토큰이 재발급되었습니다.", response);
    }

    /**
     * 로그아웃
     * - refresh token을 무효화한다.
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequestDto request) {
        authService.logout(request);
        return ApiResponse.success("로그아웃되었습니다.");
    }
}
