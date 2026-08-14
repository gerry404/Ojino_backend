package com.schoolcopilot.auth_service.web.client.dto;

import com.schoolcopilot.auth_service.domain.AuthProvider;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Connexion via un fournisseur externe.
 *
 * <p>{@code displayName} n'est utile que pour Apple, qui ne communique le nom de
 * l'utilisateur qu'une seule fois, au tout premier accord, et uniquement au SDK
 * client. Si l'application ne nous le transmet pas a ce moment-la, il est perdu.
 */
public record SocialLoginRequest(

        @NotNull(message = "Le fournisseur est obligatoire.")
        AuthProvider provider,

        @NotBlank(message = "Le token d'identite est obligatoire.")
        String idToken,

        String displayName) {
}
