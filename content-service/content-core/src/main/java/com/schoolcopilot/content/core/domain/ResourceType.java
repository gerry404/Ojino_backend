package com.schoolcopilot.content.core.domain;

/** La nature d'un support d'apprentissage. */
public enum ResourceType {

    /** Cours redige, en Markdown. */
    LESSON,

    /** Fiche de revision : l'essentiel, en court. */
    SHEET,

    /** Video hebergee ailleurs. */
    VIDEO,

    /** Lien externe. */
    LINK;

    /** Vrai si le contenu est redige chez nous plutot que pointe ailleurs. */
    public boolean isAuthored() {
        return this == LESSON || this == SHEET;
    }
}
