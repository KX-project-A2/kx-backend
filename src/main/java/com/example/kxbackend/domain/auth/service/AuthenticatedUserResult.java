package com.example.kxbackend.domain.auth.service;

import com.example.kxbackend.domain.auth.dto.response.TokenResponseDto;
import com.example.kxbackend.domain.user.entity.User;

public record AuthenticatedUserResult(
        User user,
        TokenResponseDto token
) {
}
