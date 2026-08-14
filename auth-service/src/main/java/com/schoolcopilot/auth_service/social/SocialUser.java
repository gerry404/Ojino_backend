package com.schoolcopilot.auth_service.social;

import com.schoolcopilot.auth_service.domain.AuthProvider;

/**
 * Ce qu'on retient d'un ID token Google ou Apple apres verification.
 *
 * <p>Seul {@code subject} est garanti. Apple, en particulier, ne transmet l'email
 * et le nom qu'a la toute premiere connexion, et l'email peut etre un alias de
 * relais prive {@code @privaterelay.appleid.com}.
 */
public record SocialUser(
        AuthProvider provider,
        String subject,
        String email,
        boolean emailVerified,
        String displayName,
        String pictureUrl) {
}
