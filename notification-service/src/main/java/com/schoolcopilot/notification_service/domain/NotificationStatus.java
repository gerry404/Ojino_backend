package com.schoolcopilot.notification_service.domain;

/** Le sort d'une notification. */
public enum NotificationStatus {

    /** En file, en attente de son heure d'envoi. */
    PENDING,

    /** Remise au canal avec succes. */
    SENT,

    /**
     * Echecs repetes, abandonnee.
     *
     * <p>Conservee malgre tout : savoir qu'une notification n'est jamais partie
     * vaut mieux que la voir disparaitre sans trace, surtout pour comprendre a
     * posteriori pourquoi un eleve n'a pas ete prevenu.
     */
    FAILED,

    /**
     * Ecartee avant tout envoi.
     *
     * <p>Trois raisons possibles : l'utilisateur a coupe ce type sur ce canal, le
     * plafond quotidien est atteint, ou un doublon existait deja.
     */
    SUPPRESSED
}
