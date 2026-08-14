package com.schoolcopilot.learning_service.exception;

import org.springframework.http.HttpStatus;

/** Erreurs metier du service, avec un {@code code} stable pour les clients. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
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

    public static ApiException invalidScore() {
        return new ApiException(HttpStatus.BAD_REQUEST, "invalid_score",
                "Le score doit etre compris entre 0 et 1.");
    }

    public static ApiException unknownNotion(String code) {
        return new ApiException(HttpStatus.BAD_REQUEST, "unknown_notion",
                "Notion inconnue du referentiel : " + code);
    }

    public static ApiException noMastery(String code) {
        return new ApiException(HttpStatus.NOT_FOUND, "no_mastery",
                "Aucun resultat enregistre sur la notion " + code + ".");
    }

    /** content-service injoignable : une panne, pas une faute de l'eleve. */
    public static ApiException contentUnavailable() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "content_unavailable",
                "Le programme est momentanement indisponible. Reessayez.");
    }
}
