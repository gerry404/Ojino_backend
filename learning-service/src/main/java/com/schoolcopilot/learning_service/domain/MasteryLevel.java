package com.schoolcopilot.learning_service.domain;

/**
 * Ou en est l'eleve sur une notion.
 *
 * <p>Quatre paliers, pas un score brut : c'est ce que consommeront le
 * planificateur et l'assistant, et "0,63" ne se decide pas mieux qu'"en cours
 * d'apprentissage". Le score reste disponible a cote pour qui en a besoin.
 */
public enum MasteryLevel {

    /** Aucun resultat encore : la notion n'a jamais ete evaluee. */
    UNKNOWN,

    /** Resultats bas et repetes : c'est ici que la remediation se declenche. */
    STRUGGLING,

    /** En progres, ou trop peu d'elements pour conclure. */
    LEARNING,

    /** Acquise. Ne ressortira qu'en revision espacee. */
    MASTERED
}
