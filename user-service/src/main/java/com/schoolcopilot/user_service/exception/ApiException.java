package com.schoolcopilot.user_service.exception;

import org.springframework.http.HttpStatus;

/**
 * Erreurs metier du service. Le {@code code} est stable et destine aux
 * applications clientes, qui ne lisent jamais le message.
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

    public static ApiException profileNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "profile_not_found",
                "Aucun profil pour ce compte.");
    }

    public static ApiException unknownSystem(String code) {
        return new ApiException(HttpStatus.BAD_REQUEST, "unknown_system",
                "Systeme scolaire inconnu : " + code);
    }

    public static ApiException unknownLevel(String code) {
        return new ApiException(HttpStatus.BAD_REQUEST, "unknown_level",
                "Niveau inconnu dans ce systeme : " + code);
    }

    public static ApiException unknownTrack(String code) {
        return new ApiException(HttpStatus.BAD_REQUEST, "unknown_track",
                "Filiere inconnue dans ce systeme : " + code);
    }

    public static ApiException trackNotAvailable(String track, String level) {
        return new ApiException(HttpStatus.BAD_REQUEST, "track_not_available",
                "La filiere " + track + " n'existe pas en " + level + ".");
    }

    public static ApiException trackNotApplicable(String level) {
        return new ApiException(HttpStatus.BAD_REQUEST, "track_not_applicable",
                "Le niveau " + level + " ne se decline pas en filieres.");
    }

    public static ApiException unknownSubjects(Object codes) {
        return new ApiException(HttpStatus.BAD_REQUEST, "unknown_subjects",
                "Matieres inconnues pour ce niveau : " + codes);
    }

    public static ApiException stepOutOfOrder(String step, String required) {
        return new ApiException(HttpStatus.CONFLICT, "step_out_of_order",
                "L'etape " + step + " demande d'avoir renseigne " + required + " avant.");
    }

    public static ApiException invalidAvailability(String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, "invalid_availability", detail);
    }

    public static ApiException invalidBirthDate(String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, "invalid_birth_date", detail);
    }

    // ------------------------------------------------------------------
    // Back-office du referentiel
    // ------------------------------------------------------------------

    public static ApiException alreadyExists(String kind, String code) {
        return new ApiException(HttpStatus.CONFLICT, "already_exists",
                kind + " " + code + " existe deja dans ce systeme.");
    }

    /**
     * Refuse de supprimer un element encore utilise : les profils concernes
     * pointeraient vers un code disparu, et rien ne les reparerait.
     */
    public static ApiException referenceInUse(String kind, String code, long profiles) {
        return new ApiException(HttpStatus.CONFLICT, "reference_in_use",
                kind + " " + code + " est utilise par " + profiles
                        + " profil(s). Desactivez le systeme ou migrez ces profils d'abord.");
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
