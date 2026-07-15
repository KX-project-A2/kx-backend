package com.example.kxbackend.global.security;

import com.example.kxbackend.domain.auth.dto.response.TokenResponseDto;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthCookieService {

    public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final boolean secure;
    private final String sameSite;

    public AuthCookieService(
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration,
            @Value("${app.auth-cookie.secure}") boolean secure,
            @Value("${app.auth-cookie.same-site}") String sameSite
    ) {
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.secure = secure;
        this.sameSite = sameSite;
    }

    public void addTokenCookies(HttpServletResponse response, TokenResponseDto token) {
        addCookie(response, ACCESS_TOKEN_COOKIE_NAME, token.accessToken(), Duration.ofMillis(accessTokenExpiration));
        addCookie(response, REFRESH_TOKEN_COOKIE_NAME, token.refreshToken(), Duration.ofMillis(refreshTokenExpiration));
    }

    public void deleteTokenCookies(HttpServletResponse response) {
        addCookie(response, ACCESS_TOKEN_COOKIE_NAME, "", Duration.ZERO);
        addCookie(response, REFRESH_TOKEN_COOKIE_NAME, "", Duration.ZERO);
    }

    private void addCookie(HttpServletResponse response, String name, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
