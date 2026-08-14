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

    public static ApiException trackNotAvailable(String track, String level) {
        return new ApiException(HttpStatus.BAD_REQUEST, "track_not_available",
                "La filiere " + track + " n'existe pas en " + level + ".");
    }

    /**
     * L'etape ne concerne pas le cycle de cet eleve. Sans ce refus, un appel
     * direct a l'API pourrait attribuer une filiere de Terminale a un enfant de
     * maternelle.
     */
    public static ApiException stepNotApplicable(String step, String level) {
        return new ApiException(HttpStatus.BAD_REQUEST, "step_not_applicable",
                "L'etape " + step + " ne concerne pas le cycle du niveau " + level + ".");
    }

    public static ApiException unknownProgram(String code) {
        return new ApiException(HttpStatus.BAD_REQUEST, "unknown_program",
                "Parcours inconnu dans ce systeme : " + code);
    }

    public static ApiException unknownSemester(int semester, int semesterCount) {
        return new ApiException(HttpStatus.BAD_REQUEST, "unknown_semester",
                "Ce parcours compte " + semesterCount + " semestres, le " + semester
                        + " n'existe pas.");
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

    /**
     * Le referentiel est injoignable. C'est une panne, pas une faute de l'eleve :
     * on renvoie 503 pour que le client sache qu'il peut reessayer tel quel.
     */
    public static ApiException contentUnavailable() {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "content_unavailable",
                "Le referentiel scolaire est momentanement indisponible. Reessayez.");
    }
}
