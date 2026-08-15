package com.schoolcopilot.engagement_service.domain;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Comment l'eleve se sent, un jour donne.
 *
 * <p>Deux mesures distinctes, et c'est important : on peut aller bien et etre
 * debordee, ou aller mal sans surcharge de travail. Les confondre ferait passer a
 * cote de la moitie des situations.
 *
 * @param mood de 1 (tres mal) a 5 (tres bien)
 * @param workload de 1 (rien a faire) a 5 (deborde)
 * @param note libre, facultative. Souvent la partie la plus utile, et la seule
 *        qu'un adulte devrait lire.
 */
@Document(collection = "mood_checkins")
@CompoundIndex(name = "idx_mood_user_day", def = "{'userId': 1, 'day': -1}", unique = true)
public record MoodCheckIn(
        @Id String id,
        @Indexed String userId,
        LocalDate day,
        int mood,
        int workload,
        String note,
        Instant createdAt) {

    public static final int MIN = 1;
    public static final int MAX = 5;

    public boolean isValid() {
        return mood >= MIN && mood <= MAX && workload >= MIN && workload <= MAX;
    }

    /** Identifiant deterministe : un seul releve par jour, le dernier fait foi. */
    public static String idFor(String userId, LocalDate day) {
        return userId + ":" + day;
    }
}
