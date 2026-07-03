package com.example.kxbackend.global.security;

import com.example.kxbackend.domain.user.entity.enums.Role;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    public String createAccessToken(Long userId, Role role) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public Long getUserId(String token) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public boolean validateToken(String token) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
