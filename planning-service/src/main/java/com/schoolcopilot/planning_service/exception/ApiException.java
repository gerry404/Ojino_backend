package com.schoolcopilot.planning_service.exception;

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

    public static ApiException deadlineNotFound(String id) {
        return new ApiException(HttpStatus.NOT_FOUND, "deadline_not_found",
                "Echeance introuvable : " + id);
    }

    public static ApiException sessionNotFound(String id) {
        return new ApiException(HttpStatus.NOT_FOUND, "session_not_found",
                "Seance introuvable : " + id);
    }

    /**
     * L'appelant tente d'agir sur une echeance ou une seance qui ne lui appartient
     * pas. Sans ce controle, deviner un identifiant suffirait a modifier le
     * planning de quelqu'un d'autre.
     */
    public static ApiException notYours() {
        return new ApiException(HttpStatus.FORBIDDEN, "not_yours",
                "Cet element ne vous appartient pas.");
    }

    public static ApiException dueDateInPast() {
        return new ApiException(HttpStatus.BAD_REQUEST, "due_date_in_past",
                "Une echeance ne peut pas etre datee dans le passe.");
    }

    public static ApiException noAvailability() {
        return new ApiException(HttpStatus.CONFLICT, "no_availability",
                "Aucun creneau de travail declare. Completez vos disponibilites d'abord.");
    }

    public static ApiException profileUnavailable() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "profile_unavailable",
                "Votre profil est momentanement indisponible. Reessayez.");
    }

    public static ApiException invalidTransition(String from, String to) {
        return new ApiException(HttpStatus.CONFLICT, "invalid_transition",
                "Une seance " + from + " ne peut pas passer a " + to + ".");
    }
}
