package com.schoolcopilot.content.core.domain;

/**
 * Les grands cycles scolaires.
 *
 * <p>Ce n'est pas un simple libelle : chaque cycle a son module Maven, son
 * vocabulaire et ses regles. Un eleve de maternelle n'a pas de filiere, un
 * etudiant n'a pas de classe mais des unites d'enseignement et des credits.
 * Plaquer le modele du lycee sur tout le monde produirait un referentiel faux
 * partout sauf au lycee.
 */
public enum EducationCycle {

    /**
     * Maternelle jusqu'au CP.
     *
     * <p>Destine a devenir une application a part entiere : rien d'autre ne doit
     * dependre de {@code content-earlyyears}.
     */
    EARLY_YEARS,

    /** Collège / secondary : matieres, pas encore de specialisation partout. */
    COLLEGE,

    /** Lycee / high school : filieres et examens de fin de cycle. */
    HIGH_SCHOOL,

    /** Classes preparatoires : filieres denses tournees vers des concours. */
    PREPA,

    /** Superieur : parcours, semestres, unites d'enseignement et credits. */
    UNIVERSITY
}
