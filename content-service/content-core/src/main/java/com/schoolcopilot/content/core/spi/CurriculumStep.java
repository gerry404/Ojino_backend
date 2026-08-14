package com.schoolcopilot.content.core.spi;

/**
 * Un choix que le cycle demande a l'eleve, en plus de sa classe.
 *
 * <p>C'est ce qui permet au parcours d'inscription de {@code user-service} de
 * s'adapter sans connaitre les cycles : il demande a content-service quelles
 * etapes suivent le choix du niveau, et les enchaine.
 */
public enum CurriculumStep {

    /** Filiere : A4, C, D, Science, Commercial... */
    TRACK,

    /** Matieres suivies. */
    SUBJECTS,

    /** Domaines d'apprentissage, a la place des matieres avant le primaire. */
    LEARNING_DOMAINS,

    /** Parcours universitaire : licence d'informatique, master de droit... */
    PROGRAM,

    /** Semestre en cours. */
    SEMESTER,

    /** Unites d'enseignement. */
    COURSE_UNITS
}
