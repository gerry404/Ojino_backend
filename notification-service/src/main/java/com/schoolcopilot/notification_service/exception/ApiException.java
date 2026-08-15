package com.schoolcopilot.notification_service.exception;

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

    public static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "notification_not_found",
                "Notification introuvable.");
    }

    public static ApiException unknownZone(String zone) {
        return new ApiException(HttpStatus.BAD_REQUEST, "unknown_zone",
                "Fuseau horaire inconnu : " + zone);
    }
}
