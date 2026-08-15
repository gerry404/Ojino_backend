package com.schoolcopilot.engagement_service.domain;

/**
 * Les accomplissements reconnus.
 *
 * <p>Volontairement peu nombreux et centres sur l'effort plutot que sur le
 * resultat : recompenser la regularite encourage tout le monde, recompenser les
 * notes ne recompense que ceux qui reussissent deja.
 *
 * @param threshold valeur a atteindre sur le compteur correspondant
 */
public enum Badge {

    FIRST_SESSION(Metric.SESSIONS_DONE, 1, "Premiere seance"),
    TEN_SESSIONS(Metric.SESSIONS_DONE, 10, "Dix seances"),
    FIFTY_SESSIONS(Metric.SESSIONS_DONE, 50, "Cinquante seances"),

    STREAK_3(Metric.STREAK, 3, "Trois jours d'affilee"),
    STREAK_7(Metric.STREAK, 7, "Une semaine complete"),
    STREAK_30(Metric.STREAK, 30, "Un mois complet"),

    FIRST_NOTION(Metric.NOTIONS_MASTERED, 1, "Premiere notion acquise"),
    TEN_NOTIONS(Metric.NOTIONS_MASTERED, 10, "Dix notions acquises"),

    /** Reprendre apres une interruption merite d'etre salue, pas passe sous silence. */
    COMEBACK(Metric.COMEBACKS, 1, "Retour apres une pause");

    private final Metric metric;
    private final int threshold;
    private final String label;

    Badge(Metric metric, int threshold, String label) {
        this.metric = metric;
        this.threshold = threshold;
        this.label = label;
    }

    public Metric metric() {
        return metric;
    }

    public int threshold() {
        return threshold;
    }

    public String label() {
        return label;
    }

    public boolean isEarnedAt(int value) {
        return value >= threshold;
    }

    /** Les compteurs sur lesquels les badges s'appuient. */
    public enum Metric {
        SESSIONS_DONE,
        STREAK,
        NOTIONS_MASTERED,
        COMEBACKS
    }
}
