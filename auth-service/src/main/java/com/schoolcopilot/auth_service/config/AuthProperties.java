package com.schoolcopilot.auth_service.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tous les reglages d'authentification, regroupes sous le prefixe {@code ojino.auth}.
 */
@ConfigurationProperties(prefix = "ojino.auth")
public record AuthProperties(
        Jwt jwt,
        Refresh refresh,
        Cookie cookie,
        Otp otp,
        Social google,
        Social apple,
        Cors cors) {

    /** Signature des access tokens que nous emettons nous-memes. */
    public record Jwt(String secret, String issuer, Duration accessTokenTtl) {
    }

    /**
     * Durees de vie des refresh tokens. Elles sont glissantes : chaque rotation
     * repart d'une duree pleine, donc un utilisateur qui ouvre l'app de temps en
     * temps ne se reconnecte jamais.
     */
    public record Refresh(Duration mobileTtl, Duration webTtl) {
    }

    /** Cookie porteur du refresh token cote web. */
    public record Cookie(String name, String path, String sameSite, boolean secure, String domain) {
    }

    /**
     * Connexion par code SMS. {@code exposeCode} renvoie le code dans la reponse
     * HTTP pour pouvoir developper sans fournisseur SMS : a laisser a false
     * partout ailleurs qu'en local.
     */
    public record Otp(int length, Duration ttl, int maxAttempts, Duration resendCooldown,
            boolean exposeCode) {
    }

    /**
     * Un fournisseur d'identite externe. {@code clientIds} liste les audiences
     * acceptees : un client ID par plateforme (web, iOS, Android).
     */
    public record Social(List<String> clientIds) {

        public boolean isConfigured() {
            return clientIds != null && !clientIds.isEmpty();
        }
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
