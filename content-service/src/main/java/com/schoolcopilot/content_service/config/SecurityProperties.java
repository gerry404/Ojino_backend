package com.schoolcopilot.content_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ojino.security")
public record SecurityProperties(Jwt jwt) {

    /**
     * Le secret est le meme que celui de l'auth-service. Ce partage evite d'appeler
     * l'auth-service a chaque requete : la signature suffit a prouver l'identite.
     */
    public record Jwt(String secret, String issuer) {
    }
}
