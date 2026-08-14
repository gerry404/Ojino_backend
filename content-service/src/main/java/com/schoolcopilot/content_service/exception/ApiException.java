package com.schoolcopilot.content_service.exception;

import org.springframework.http.HttpStatus;

/**
 * Erreurs metier du service. Le {@code code} est stable et destine aux
 * appelants — applications clientes comme autres microservices — qui ne lisent
 * jamais le message.
 */
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

    public static ApiException unknownSystem(String code) {
        return new ApiException(HttpStatus.NOT_FOUND, "unknown_system",
                "Systeme scolaire inconnu : " + code);
    }

    public static ApiException unknownLevel(String code) {
        return new ApiException(HttpStatus.NOT_FOUND, "unknown_level",
                "Niveau inconnu dans ce systeme : " + code);
    }

    public static ApiException unknownTrack(String code) {
        return new ApiException(HttpStatus.NOT_FOUND, "unknown_track",
                "Filiere inconnue dans ce systeme : " + code);
    }

    public static ApiException unknownSubjects(Object codes) {
        return new ApiException(HttpStatus.BAD_REQUEST, "unknown_subjects",
                "Matieres inconnues pour ce niveau : " + codes);
    }

    public static ApiException trackNotAvailable(String track, String level) {
        return new ApiException(HttpStatus.BAD_REQUEST, "track_not_available",
                "La filiere " + track + " n'existe pas en " + level + ".");
    }

    /** L'element existe encore mais n'est plus proposable a un nouveau choix. */
    public static ApiException archived(String kind, String code) {
        return new ApiException(HttpStatus.CONFLICT, "archived",
                kind + " " + code + " a ete archive et ne peut plus etre choisi.");
    }

    public static ApiException alreadyExists(String kind, String code) {
        return new ApiException(HttpStatus.CONFLICT, "already_exists",
                kind + " " + code + " existe deja dans ce systeme.");
    }

    public static ApiException invalidAgeRange() {
        return new ApiException(HttpStatus.BAD_REQUEST, "invalid_age_range",
                "L'age minimum ne peut pas depasser l'age maximum.");
    }

    public static ApiException unknownLevelsReferenced(Object codes) {
        return new ApiException(HttpStatus.BAD_REQUEST, "unknown_levels_referenced",
                "Niveaux inconnus dans ce systeme : " + codes);
    }
}
