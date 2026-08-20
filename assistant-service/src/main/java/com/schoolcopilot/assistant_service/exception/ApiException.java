package com.schoolcopilot.assistant_service.exception;

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

    public static ApiException conversationNotFound(String id) {
        return new ApiException(HttpStatus.NOT_FOUND, "conversation_not_found",
                "Conversation introuvable : " + id);
    }

    public static ApiException messageNotFound(String id) {
        return new ApiException(HttpStatus.NOT_FOUND, "message_not_found",
                "Message introuvable : " + id);
    }

    /**
     * La conversation appartient a quelqu'un d'autre.
     *
     * <p>Une conversation avec l'assistant est ce qu'il y a de plus personnel dans
     * ce produit : un identifiant devine ne doit rien ouvrir.
     */
    public static ApiException notYours() {
        return new ApiException(HttpStatus.FORBIDDEN, "not_yours",
                "Cette conversation ne vous appartient pas.");
    }

    /** Le profil est injoignable : sans lui, pas de registre de langage. */
    public static ApiException profileUnavailable() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "profile_unavailable",
                "Ton profil est momentanement indisponible. Reessaie dans un instant.");
    }

    public static ApiException quotaExceeded(String code, String reason) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, code, reason);
    }

    public static ApiException engineUnavailable() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "engine_unavailable",
                "L'assistant est momentanement indisponible. Reessaie dans un instant.");
    }

    public static ApiException emptyQuestion() {
        return new ApiException(HttpStatus.BAD_REQUEST, "empty_question",
                "La question est vide.");
    }
}
