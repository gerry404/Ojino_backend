package com.schoolcopilot.auth_service.exception;

import org.springframework.http.HttpStatus;

/**
 * Toutes les erreurs metier de l'authentification.
 *
 * <p>Le {@code code} est une chaine stable destinee aux applications clientes :
 * elles s'en servent pour afficher le bon message, sans jamais parser le texte.
 */
public class AuthException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public AuthException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    /**
     * Volontairement identique que l'email soit inconnu ou le mot de passe faux :
     * sinon l'endpoint permet de deviner qui possede un compte.
     */
    public static AuthException invalidCredentials() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "invalid_credentials",
                "Identifiants incorrects.");
    }

    public static AuthException emailAlreadyUsed() {
        return new AuthException(HttpStatus.CONFLICT, "email_already_used",
                "Un compte existe deja avec cette adresse email.");
    }

    public static AuthException noPasswordSet() {
        return new AuthException(HttpStatus.CONFLICT, "no_password_set",
                "Ce compte a ete cree via Google, Apple ou SMS. Utilisez la meme methode pour vous connecter.");
    }

    public static AuthException accountDisabled() {
        return new AuthException(HttpStatus.FORBIDDEN, "account_disabled",
                "Ce compte a ete desactive.");
    }

    public static AuthException invalidRefreshToken() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "invalid_refresh_token",
                "Session expiree, reconnexion necessaire.");
    }

    public static AuthException providerNotConfigured(String provider) {
        return new AuthException(HttpStatus.SERVICE_UNAVAILABLE, "provider_not_configured",
                "La connexion " + provider + " n'est pas encore configuree sur ce serveur.");
    }

    public static AuthException invalidSocialToken(String provider) {
        return new AuthException(HttpStatus.UNAUTHORIZED, "invalid_social_token",
                "Le token " + provider + " est invalide ou a expire.");
    }

    public static AuthException otpThrottled(long retryAfterSeconds) {
        return new AuthException(HttpStatus.TOO_MANY_REQUESTS, "otp_throttled",
                "Un code vient d'etre envoye. Reessayez dans " + retryAfterSeconds + " secondes.");
    }

    public static AuthException otpInvalid() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "otp_invalid",
                "Code incorrect ou expire.");
    }

    public static AuthException otpTooManyAttempts() {
        return new AuthException(HttpStatus.TOO_MANY_REQUESTS, "otp_too_many_attempts",
                "Trop de tentatives. Demandez un nouveau code.");
    }

    public static AuthException userNotFound() {
        return new AuthException(HttpStatus.NOT_FOUND, "user_not_found",
                "Compte introuvable.");
    }
}
