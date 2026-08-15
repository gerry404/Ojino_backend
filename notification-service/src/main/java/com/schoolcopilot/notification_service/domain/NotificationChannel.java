package com.schoolcopilot.notification_service.domain;

/** Par ou passe une notification. */
public enum NotificationChannel {

    /** Notification push mobile (FCM ou APNs). */
    PUSH,

    /** Courrier electronique. */
    EMAIL,

    /**
     * Message dans l'application, consulte quand l'utilisateur l'ouvre.
     *
     * <p>Ne derange personne : il n'est donc soumis ni aux heures de silence ni au
     * plafond quotidien. C'est aussi le canal de repli quand tous les autres sont
     * coupes — une notification doit rester consultable quelque part.
     */
    IN_APP;

    public boolean isIntrusive() {
        return this != IN_APP;
    }
}
