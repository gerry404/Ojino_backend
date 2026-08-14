package com.schoolcopilot.auth_service.web.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.schoolcopilot.auth_service.domain.AuthProvider;
import com.schoolcopilot.auth_service.domain.LinkedIdentity;
import com.schoolcopilot.auth_service.domain.User;

/**
 * Vue back-office d'un compte.
 *
 * <p>Plus detaillee que celle rendue aux applications : elle montre l'etat
 * d'activation, la derniere connexion et le detail des identites rattachees, dont
 * un utilisateur ordinaire n'a pas besoin. Le hash du mot de passe n'y figure
 * jamais, un administrateur n'a aucune raison de le voir.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AdminUserResponse(
        String id,
        String email,
        boolean emailVerified,
        String phone,
        boolean phoneVerified,
        String displayName,
        String avatarUrl,
        Set<String> roles,
        List<IdentityView> identities,
        boolean hasPassword,
        boolean disabled,
        Instant createdAt,
        Instant updatedAt,
        Instant lastLoginAt) {

    public record IdentityView(AuthProvider provider, String subject, Instant linkedAt) {
    }

    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.isEmailVerified(),
                user.getPhone(),
                user.isPhoneVerified(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getRoles(),
                user.getIdentities().stream().map(AdminUserResponse::toView).toList(),
                user.getPasswordHash() != null,
                user.isDisabled(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getLastLoginAt());
    }

    private static IdentityView toView(LinkedIdentity identity) {
        return new IdentityView(identity.provider(), identity.subject(), identity.linkedAt());
    }
}
