package com.schoolcopilot.planning_service.domain;

/** La nature d'une echeance. */
public enum DeadlineType {

    /** Devoir a rendre. */
    HOMEWORK,

    /** Interrogation ou controle continu. */
    QUIZ,

    /** Devoir surveille. */
    EXAM,

    /** Examen officiel : BEPC, Probatoire, Baccalaureat, GCE. */
    OFFICIAL_EXAM,

    /** Concours d'entree. */
    CONTEST;

    /**
     * Nombre de jours avant lequel il faut commencer a reviser.
     *
     * <p>Un examen officiel se prepare sur des semaines, une interrogation sur
     * quelques jours. C'est ce qui evite un planning qui ne se declenche que la
     * veille, quand il est trop tard pour rattraper quoi que ce soit.
     */
    public int preparationDays() {
        return switch (this) {
            case HOMEWORK -> 3;
            case QUIZ -> 5;
            case EXAM -> 10;
            case OFFICIAL_EXAM, CONTEST -> 30;
        };
    }
}
