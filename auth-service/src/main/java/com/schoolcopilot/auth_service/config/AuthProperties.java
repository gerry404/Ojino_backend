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
        Cors cors,
        List<String> adminEmails) {

    /**
     * Les comptes portant l'une de ces adresses recoivent le role {@code ADMIN} a
     * la connexion.
     *
     * <p>Sans cela, personne ne pourrait jamais devenir administrateur : il faut
     * deja etre admin pour appeler l'API qui promeut les autres. L'attribution est
     * uniquement additive — retirer une adresse d'ici ne retire pas le role, cela
     * se fait par l'API d'administration.
     */
    public List<String> adminEmails() {
        return adminEmails == null ? List.of() : adminEmails;
    }

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
