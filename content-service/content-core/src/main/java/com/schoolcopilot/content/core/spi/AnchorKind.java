package com.schoolcopilot.content.core.spi;

/**
 * Ce a quoi un chapitre se rattache, selon le cycle.
 *
 * <p>Le programme se decoupe partout de la meme facon — chapitres, puis notions —
 * mais ce qui le porte change completement d'un cycle a l'autre. Un chapitre de
 * Terminale appartient a une matiere, un chapitre de grande section a un domaine
 * d'apprentissage, un chapitre de licence a une unite d'enseignement.
 *
 * <p>C'est la seule chose que le coeur a besoin de savoir : il ignore ce qu'est
 * une matiere ou une UE, il demande au module du cycle si le code d'ancrage
 * existe.
 */
public enum AnchorKind {

    /** College, lycee, prepa : une matiere. */
    SUBJECT,

    /** Maternelle et CP : un domaine d'apprentissage. */
    LEARNING_DOMAIN,

    /** Superieur : une unite d'enseignement. */
    COURSE_UNIT
}
