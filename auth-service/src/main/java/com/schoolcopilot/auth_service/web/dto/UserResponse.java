package com.schoolcopilot.auth_service.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.schoolcopilot.auth_service.domain.AuthProvider;
import com.schoolcopilot.auth_service.domain.LinkedIdentity;
import com.schoolcopilot.auth_service.domain.User;

/**
 * Le compte tel que les applications le voient. Ne contient jamais le hash du
 * mot de passe.
 */
public record UserResponse(
        String id,
        String email,
        boolean emailVerified,
        String phone,
        boolean phoneVerified,
        String displayName,
        String avatarUrl,
        Set<String> roles,
        List<AuthProvider> providers,
        boolean hasPassword,
        Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.isEmailVerified(),
                user.getPhone(),
                user.isPhoneVerified(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getRoles(),
                user.getIdentities().stream().map(LinkedIdentity::provider).distinct().toList(),
                user.getPasswordHash() != null,
                user.getCreatedAt());
    }
}
