package com.example.kxbackend.global.security;

import com.example.kxbackend.domain.auth.dto.response.TokenResponseDto;
import com.example.kxbackend.domain.auth.service.AuthTokenService;
import com.example.kxbackend.domain.auth.service.GoogleOAuth2UserService;
import com.example.kxbackend.domain.user.entity.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final GoogleOAuth2UserService googleOAuth2UserService;
    private final AuthTokenService authTokenService;
    private final AuthCookieService authCookieService;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        User user = googleOAuth2UserService.findOrCreateUser(oAuth2User.getAttributes());
        TokenResponseDto token = authTokenService.issueTokens(user);
        authCookieService.addTokenCookies(response, token);

        response.sendRedirect(redirectUri);
    }
}
