package com.schoolcopilot.auth_service.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Reponse commune a toutes les connexions.
 *
 * <p>{@code refreshToken} n'est present que pour le mobile ; cote web il est
 * absent puisqu'il est depose dans un cookie httpOnly.
 *
 * <p>{@code newAccount} vaut true quand le compte vient d'etre cree : c'est le
 * signal pour l'application de lancer le parcours de creation de profil.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        boolean newAccount,
        UserResponse user) {

    public static AuthResponse of(String accessToken, long expiresIn, String refreshToken,
            boolean newAccount, UserResponse user) {
        return new AuthResponse(accessToken, "Bearer", expiresIn, refreshToken, newAccount, user);
    }
}
