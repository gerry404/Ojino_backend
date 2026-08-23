package com.schoolcopilot.support_service.exception;

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

    public static ApiException faqEntryNotFound(String code) {
        return new ApiException(HttpStatus.NOT_FOUND, "faq_entry_not_found",
                "Entree de FAQ introuvable : " + code);
    }

    /**
     * 409 et non 400 : la requete est valide, c'est l'etat du serveur qui la rend
     * impossible. La nuance decide si le client doit corriger sa saisie ou
     * reessayer.
     */
    public static ApiException faqCodeAlreadyExists(String code) {
        return new ApiException(HttpStatus.CONFLICT, "faq_code_already_exists",
                "Ce code est deja utilise : " + code);
    }
}
