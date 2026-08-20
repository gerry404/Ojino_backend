package com.schoolcopilot.assistant_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param internalToken secret partage protegeant les routes internes, appelees
 *        par les autres services depuis des taches planifiees — sans jeton
 *        d'utilisateur sous la main. Solution d'attente, voir
 *        {@link InternalApiFilter}.
 */
@ConfigurationProperties(prefix = "ojino.security")
public record SecurityProperties(Jwt jwt, String internalToken) {

    /**
     * Le secret est le meme que celui de l'auth-service. Ce partage evite d'appeler
     * l'auth-service a chaque requete : la signature suffit a prouver l'identite.
     */
    public record Jwt(String secret, String issuer) {
    }
}
