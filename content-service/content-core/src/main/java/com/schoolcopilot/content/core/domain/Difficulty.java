package com.schoolcopilot.content.core.domain;

/**
 * Le niveau d'un exercice, relatif a la notion qu'il travaille.
 *
 * <p>Trois paliers volontairement : au-dela, personne ne sait plus ou placer la
 * limite, et le classement devient arbitraire.
 */
public enum Difficulty {

    /** Application directe du cours. */
    EASY,

    /** Demande de combiner deux idees. */
    MEDIUM,

    /** Demande une initiative ou un detour. */
    HARD
}
