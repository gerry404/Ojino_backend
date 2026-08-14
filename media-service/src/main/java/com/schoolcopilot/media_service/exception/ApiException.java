package com.schoolcopilot.media_service.exception;

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

    public static ApiException notFound(String id) {
        return new ApiException(HttpStatus.NOT_FOUND, "media_not_found",
                "Fichier introuvable : " + id);
    }

    /**
     * Le fichier existe mais appartient a quelqu'un d'autre.
     *
     * <p>Sans ce controle, deviner un identifiant suffirait a lire le devoir d'un
     * autre eleve.
     */
    public static ApiException notYours() {
        return new ApiException(HttpStatus.FORBIDDEN, "not_yours",
                "Ce fichier ne vous appartient pas.");
    }

    public static ApiException unsupportedType(String contentType, Object allowed) {
        return new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported_content_type",
                "Type de fichier refuse : " + contentType + ". Types acceptes : " + allowed);
    }

    public static ApiException tooLarge(long bytes, long maxBytes) {
        return new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "file_too_large",
                "Fichier de " + bytes + " octets, maximum " + maxBytes + ".");
    }

    public static ApiException notUploaded() {
        return new ApiException(HttpStatus.CONFLICT, "not_uploaded",
                "Aucun fichier n'a ete recu pour cette demande.");
    }

    public static ApiException alreadyConfirmed() {
        return new ApiException(HttpStatus.CONFLICT, "already_confirmed",
                "Ce fichier a deja ete confirme.");
    }

    public static ApiException notReady() {
        return new ApiException(HttpStatus.CONFLICT, "not_ready",
                "Ce fichier n'a pas encore ete confirme.");
    }

    /** Signature invalide ou expiree sur une adresse de stockage. */
    public static ApiException invalidLink() {
        return new ApiException(HttpStatus.FORBIDDEN, "invalid_link",
                "Lien invalide ou expire.");
    }

    /**
     * La taille reelle ne correspond pas a celle annoncee.
     *
     * <p>Un client qui annonce cent kilo-octets et en envoie cinquante mega
     * contournerait autrement toute limite.
     */
    public static ApiException sizeMismatch(long declared, long actual) {
        return new ApiException(HttpStatus.BAD_REQUEST, "size_mismatch",
                "Taille annoncee " + declared + " octets, recue " + actual + ".");
    }
}
