package com.example.kxbackend.domain.auth.service;

import com.example.kxbackend.domain.user.entity.User;
import com.example.kxbackend.domain.user.entity.enums.AuthProvider;
import com.example.kxbackend.domain.user.entity.enums.Role;
import com.example.kxbackend.domain.user.repository.UserRepository;
import com.example.kxbackend.global.exception.BusinessException;
import com.example.kxbackend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoogleOAuth2UserService {

    private static final String GOOGLE_SUB_ATTRIBUTE = "sub";
    private static final String GOOGLE_EMAIL_ATTRIBUTE = "email";
    private static final String GOOGLE_NAME_ATTRIBUTE = "name";

    private final UserRepository userRepository;

    /**
     * Google OAuth 사용자 정보를 서비스의 User로 변환한다.
     */
    @Transactional
    public User findOrCreateUser(Map<String, Object> attributes) {
        String providerId = getRequiredAttribute(attributes, GOOGLE_SUB_ATTRIBUTE);
        String email = getRequiredAttribute(attributes, GOOGLE_EMAIL_ATTRIBUTE);
        String name = getOptionalAttribute(attributes, GOOGLE_NAME_ATTRIBUTE);

        return userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, providerId)
                .orElseGet(() -> createGoogleUser(providerId, email, name));
    }

    private User createGoogleUser(String providerId, String email, String name) {
        userRepository.findByEmail(email)
                .ifPresent(user -> {
                    throw new BusinessException(ErrorCode.OAUTH_EMAIL_ALREADY_EXISTS);
                });

        User user = User.builder()
                .email(email)
                .nickname(resolveNickname(email, name))
                .provider(AuthProvider.GOOGLE)
                .providerId(providerId)
                .role(Role.USER)
                .build();

        return userRepository.save(user);
    }

    private String resolveNickname(String email, String name) {
        if (StringUtils.hasText(name)) {
            return name;
        }

        int atIndex = email.indexOf('@');
        if (atIndex > 0) {
            return email.substring(0, atIndex);
        }

        return email;
    }

    private String getRequiredAttribute(Map<String, Object> attributes, String key) {
        String value = getOptionalAttribute(attributes, key);
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.INVALID_OAUTH2_USER);
        }

        return value;
    }

    private String getOptionalAttribute(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value instanceof String stringValue ? stringValue : null;
    }
}
