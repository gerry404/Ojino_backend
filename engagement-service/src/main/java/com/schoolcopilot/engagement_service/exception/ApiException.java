package com.schoolcopilot.engagement_service.exception;

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

    public static ApiException invalidCheckIn() {
        return new ApiException(HttpStatus.BAD_REQUEST, "invalid_checkin",
                "L'humeur et la charge se notent de 1 a 5.");
    }

    public static ApiException futureDate() {
        return new ApiException(HttpStatus.BAD_REQUEST, "future_date",
                "Impossible d'enregistrer une activite dans le futur.");
    }
}
