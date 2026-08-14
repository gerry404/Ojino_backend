package com.schoolcopilot.media_service.domain;

/**
 * Ou en est un fichier.
 *
 * <p>L'envoi se fait en deux temps : le service delivre une adresse d'envoi, puis
 * le client confirme une fois le transfert termine. Sans cette confirmation, on
 * ne saurait pas distinguer un fichier reellement depose d'une adresse delivree
 * pour rien — et la base se remplirait de references vers des fichiers
 * inexistants.
 */
public enum MediaStatus {

    /** Adresse d'envoi delivree, transfert pas encore confirme. */
    PENDING,

    /** Transfert confirme, le fichier est utilisable. */
    READY,

    /**
     * Marque pour suppression.
     *
     * <p>Le document survit brievement a l'effacement du fichier : si la
     * suppression du stockage echoue, on sait quoi reessayer. Passer directement
     * a la suppression laisserait des fichiers orphelins dans le stockage, sans
     * trace pour les retrouver.
     */
    DELETED
}
