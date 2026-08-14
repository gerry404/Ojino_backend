package com.schoolcopilot.planning_service.domain;

/** Ou en est une seance de travail. */
public enum SessionStatus {

    /** Prevue, pas encore commencee. */
    PLANNED,

    /** En cours. */
    IN_PROGRESS,

    /** Terminee. */
    DONE,

    /**
     * Non faite a la date prevue, et reportee.
     *
     * <p>Distinct de {@link #CANCELLED} : une seance manquee sera reproposee, une
     * seance annulee ne revient pas. Confondre les deux ferait disparaitre du
     * travail que l'eleve doit encore faire.
     */
    MISSED,

    /** Ecartee volontairement par l'eleve. */
    CANCELLED
}
