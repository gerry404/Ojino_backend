package com.schoolcopilot.engagement_service.domain;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * La serie d'activite d'un eleve.
 *
 * <p><strong>Une serie doit encourager, pas punir.</strong> Chez un enfant, voir
 * quarante jours d'efforts remis a zero pour une soiree manquee produit
 * exactement l'inverse de l'effet recherche : il abandonne. D'ou les jokers, qui
 * absorbent les jours manques avant que la serie ne casse.
 *
 * @param freezesAvailable jokers en reserve, consommes automatiquement
 * @param longest jamais remis a zero : meme une serie cassee laisse une trace de
 *        ce dont l'eleve a ete capable
 * @param freezesRefilledOn date du dernier rechargement, pour ne pas recharger
 *        deux fois la meme semaine
 */
@Document(collection = "streaks")
public record Streak(
        @Id String userId,
        int current,
        int longest,
        LocalDate lastActiveOn,
        int freezesAvailable,
        int freezesUsedTotal,
        LocalDate freezesRefilledOn,
        Instant updatedAt) {

    public static Streak start(String userId, int initialFreezes) {
        return new Streak(userId, 0, 0, null, initialFreezes, 0, null, Instant.now());
    }

    public boolean isActiveOn(LocalDate date) {
        return date.equals(lastActiveOn);
    }

    /** Vrai si la serie tient encore compte tenu des jokers restants. */
    public boolean isAlive(LocalDate today) {
        if (lastActiveOn == null) {
            return false;
        }
        long gap = java.time.temporal.ChronoUnit.DAYS.between(lastActiveOn, today);
        return gap <= 1 + freezesAvailable;
    }
}
