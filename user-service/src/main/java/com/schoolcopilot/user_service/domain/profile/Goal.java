package com.schoolcopilot.user_service.domain.profile;

/** Ce que l'eleve vient chercher. Oriente tout ce que l'application lui proposera. */
public enum Goal {

    /** Reussir un examen officiel : BEPC, Probatoire, Baccalaureat, GCE... */
    PASS_EXAM,

    /** Remonter sa moyenne generale. */
    IMPROVE_AVERAGE,

    /** Rattraper un retard accumule. */
    CATCH_UP,

    /** Prendre de l'avance sur le programme. */
    GET_AHEAD,

    /** Preparer un concours d'entree. */
    PREPARE_CONTEST
}
