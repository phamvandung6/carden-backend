package com.loopy.carden.mapper;

import com.loopy.carden.dto.auth.AuthenticationResponse;
import com.loopy.carden.dto.auth.RegisterRequest;
import com.loopy.carden.entity.User;

import java.time.LocalDateTime;

public final class AuthMapper {

    private AuthMapper() {}

    public static User toEntity(RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setDisplayName(request.getDisplayName() != null ? request.getDisplayName() : request.getUsername());
        user.setRole(User.Role.USER);
        user.setActive(true);
        user.setEmailVerified(false); // In real app, send verification email
        user.setUiLanguage(request.getUiLanguage());
        user.setTimezone(request.getTimezone());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    public static AuthenticationResponse.UserInfo toUserInfo(User user) {
        return AuthenticationResponse.UserInfo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole().name())
                .emailVerified(user.isEmailVerified())
                .uiLanguage(user.getUiLanguage())
                .timezone(user.getTimezone())
                .build();
    }

    public static AuthenticationResponse toAuthResponse(String accessToken, User user, long expiresInSeconds) {
        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(expiresInSeconds)
                .user(toUserInfo(user))
                .timestamp(LocalDateTime.now())
                .build();
    }
}
