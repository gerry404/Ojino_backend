package com.schoolcopilot.planning_service.domain;

/**
 * Pourquoi cette seance a ete proposee.
 *
 * <p>Expose a l'eleve : un planning qui explique ses choix se suit, un planning
 * opaque s'abandonne. "Parce que tu bloques dessus" et "parce que le controle est
 * dans trois jours" ne se recoivent pas de la meme facon.
 */
public enum SessionReason {

    /** Notion en difficulte. */
    REMEDIATION,

    /** Echeance proche. */
    DEADLINE,

    /** Notion acquise qu'il faut entretenir. */
    SPACED_REVIEW,

    /** Progression normale dans le programme. */
    PROGRESSION,

    /** Rattrapage d'une seance manquee. */
    CATCH_UP,

    /** Ajoutee par l'eleve lui-meme. */
    MANUAL
}
