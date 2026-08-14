package com.schoolcopilot.content.core.domain;

/**
 * L'etat editorial d'un element de programme.
 *
 * <p>Pose des maintenant plutot qu'apres coup : une equipe pedagogique redige par
 * a-coups, et du contenu a moitie ecrit ne doit jamais atteindre un eleve.
 * Rajouter ce champ plus tard aurait demande de reprendre chaque requete de
 * lecture pour y ajouter le filtre.
 */
public enum PublicationStatus {

    /** Visible du seul back-office. */
    DRAFT,

    /** Visible des applications. */
    PUBLISHED
}
