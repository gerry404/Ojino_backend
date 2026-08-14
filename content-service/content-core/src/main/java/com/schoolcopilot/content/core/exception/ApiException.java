package com.schoolcopilot.content.core.exception;

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

    public static ApiException unknownNotion(Object code) {
        return new ApiException(HttpStatus.NOT_FOUND, "unknown_notion",
                "Notion inconnue dans ce systeme : " + code);
    }

    /**
     * Le graphe des prerequis doit rester acyclique. Un cycle rendrait l'ordre
     * d'apprentissage impossible a calculer et bloquerait l'eleve : chaque notion
     * attendrait l'autre.
     */
    public static ApiException prerequisiteCycle(String notion, Object candidates) {
        return new ApiException(HttpStatus.CONFLICT, "prerequisite_cycle",
                "Declarer " + candidates + " comme prerequis de " + notion
                        + " creerait une boucle : l'une de ces notions depend deja de " + notion + ".");
    }

    public static ApiException unknownChapter(String code) {
        return new ApiException(HttpStatus.NOT_FOUND, "unknown_chapter",
                "Chapitre inconnu dans ce systeme : " + code);
    }

    /**
     * Le code d'ancrage n'existe pas pour ce cycle. C'est le module du cycle qui
     * a repondu : le coeur ne sait pas ce qu'est une matiere ou une UE.
     */
    public static ApiException unknownAnchor(String kind, String code) {
        return new ApiException(HttpStatus.BAD_REQUEST, "unknown_anchor",
                "Aucun element de type " + kind + " ne porte le code " + code + " dans ce systeme.");
    }

    public static ApiException unknownProgram(String code) {
        return new ApiException(HttpStatus.NOT_FOUND, "unknown_program",
                "Parcours inconnu dans ce systeme : " + code);
    }

    public static ApiException unknownSemester(int semester, int semesterCount) {
        return new ApiException(HttpStatus.BAD_REQUEST, "unknown_semester",
                "Ce parcours compte " + semesterCount + " semestres, le " + semester
                        + " n'existe pas.");
    }

    public static ApiException unknownLearningDomains(Object codes) {
        return new ApiException(HttpStatus.BAD_REQUEST, "unknown_learning_domains",
                "Domaines d'apprentissage inconnus pour ce niveau : " + codes);
    }

    public static ApiException unknownSubjects(Object codes) {
        return new ApiException(HttpStatus.BAD_REQUEST, "unknown_subjects",
                "Matieres inconnues pour ce niveau : " + codes);
    }

    public static ApiException trackNotAvailable(String track, String level) {
        return new ApiException(HttpStatus.BAD_REQUEST, "track_not_available",
                "La filiere " + track + " n'existe pas en " + level + ".");
    }

    /**
     * Le cycle existe dans les donnees mais son module n'est pas embarque dans ce
     * deploiement. Mieux vaut le dire franchement que d'ouvrir un parcours que
     * l'application ne saura pas mener a bout.
     */
    public static ApiException cycleNotAvailable(String cycle) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "cycle_not_available",
                "Le cycle " + cycle + " n'est pas servi par ce deploiement.");
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
