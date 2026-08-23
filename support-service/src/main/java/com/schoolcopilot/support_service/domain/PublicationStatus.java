package com.schoolcopilot.support_service.domain;

/**
 * L'etat editorial d'une entree de FAQ.
 *
 * <p>Une equipe redige par a-coups. Sans ce statut, une reponse a moitie ecrite
 * partirait sous les yeux d'un utilisateur des la premiere sauvegarde.
 *
 * <p>Le champ se pose des la premiere version, jamais apres : l'ajouter plus
 * tard obligerait a reprendre chaque requete de lecture pour y glisser le
 * filtre, et il suffirait d'en oublier une pour publier un brouillon.
 */
public enum PublicationStatus {

    /** Redigee, pas encore relue. Invisible du parcours eleve. */
    DRAFT,

    /** Relue et mise en ligne. */
    PUBLISHED
}
