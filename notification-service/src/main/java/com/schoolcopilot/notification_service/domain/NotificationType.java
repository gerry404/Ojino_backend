package com.schoolcopilot.notification_service.domain;

/**
 * La nature d'une notification.
 *
 * <p>Le type porte ses propres regles, parce que toutes les notifications n'ont
 * pas le meme droit a deranger. Un rappel de seance peut attendre le matin ; la
 * confirmation d'un changement de mot de passe, non.
 *
 * @see Urgency
 */
public enum NotificationType {

    /** Une seance de travail commence bientot. */
    SESSION_REMINDER(Urgency.ROUTINE),

    /** Une echeance approche. */
    DEADLINE_APPROACHING(Urgency.ROUTINE),

    /** Bilan de la semaine. */
    WEEKLY_REPORT(Urgency.ROUTINE),

    /** Encouragement ou relance de motivation. */
    ENCOURAGEMENT(Urgency.ROUTINE),

    /** Serie d'activite sur le point d'etre rompue. */
    STREAK_AT_RISK(Urgency.ROUTINE),

    /** L'assistant a fini de corriger un devoir. */
    CORRECTION_READY(Urgency.PROMPT),

    /** Message d'un parent ou d'un tuteur. */
    GUARDIAN_MESSAGE(Urgency.PROMPT),

    /**
     * Securite du compte : connexion inhabituelle, mot de passe change.
     *
     * <p>Seul type qui traverse les heures de silence. Prevenir d'une intrusion le
     * lendemain matin n'a plus d'interet.
     */
    SECURITY_ALERT(Urgency.CRITICAL);

    private final Urgency urgency;

    NotificationType(Urgency urgency) {
        this.urgency = urgency;
    }

    public Urgency urgency() {
        return urgency;
    }

    /** Vrai si ce type peut etre coupe par l'utilisateur dans ses preferences. */
    public boolean isOptOutAllowed() {
        return urgency != Urgency.CRITICAL;
    }

    /** Combien une notification a le droit de deranger. */
    public enum Urgency {

        /** Peut attendre la prochaine plage autorisee, et compte dans le plafond. */
        ROUTINE,

        /** A envoyer vite, mais respecte quand meme les heures de silence. */
        PROMPT,

        /** Passe outre les heures de silence et le plafond quotidien. */
        CRITICAL
    }
}
