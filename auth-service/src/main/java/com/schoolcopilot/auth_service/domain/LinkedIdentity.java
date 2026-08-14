package com.schoolcopilot.auth_service.domain;

import java.time.Instant;

/**
 * Une facon de se connecter rattachee a un compte.
 *
 * <p>{@code subject} est l'identifiant stable fourni par le provider : le {@code sub}
 * du token pour Google et Apple, l'email pour {@link AuthProvider#EMAIL}, le numero
 * au format E.164 pour {@link AuthProvider#PHONE}.
 *
 * <p>Un meme utilisateur peut en cumuler plusieurs : c'est ce qui lui permet de
 * s'inscrire par Google puis de se reconnecter par SMS et de retrouver son compte.
 */
public record LinkedIdentity(AuthProvider provider, String subject, Instant linkedAt) {

    public static LinkedIdentity of(AuthProvider provider, String subject) {
        return new LinkedIdentity(provider, subject, Instant.now());
    }
}
