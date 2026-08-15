package com.schoolcopilot.engagement_service.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.schoolcopilot.engagement_service.config.EngagementProperties;
import com.schoolcopilot.engagement_service.domain.Streak;

/**
 * Les regles de la serie d'activite.
 *
 * <p>Logique pure, isolee a dessein : c'est la mecanique la plus sensible du
 * service. Mal reglee, elle decourage au lieu d'encourager, et le mal est fait
 * avant qu'on s'en apercoive.
 *
 * <p>Deux principes guident l'ecriture :
 * <ul>
 *   <li>un jour manque consomme un joker avant de casser quoi que ce soit ;</li>
 *   <li>meme cassee, la serie laisse le record intact — l'eleve garde la trace de
 *       ce dont il a ete capable.</li>
 * </ul>
 */
@Component
public class StreakPolicy {

    private final EngagementProperties properties;

    public StreakPolicy(EngagementProperties properties) {
        this.properties = properties;
    }

    /**
     * Ce qui est arrive a la serie, pour pouvoir l'expliquer a l'eleve.
     *
     * <p>Annoncer "ta serie continue, un joker a ete utilise" vaut infiniment mieux
     * qu'un compteur qui bouge sans raison visible.
     */
    public enum Outcome {

        /** Deja actif aujourd'hui : rien ne change. */
        ALREADY_COUNTED,

        /** Premier jour d'une serie. */
        STARTED,

        /** Jour consecutif : la serie avance. */
        CONTINUED,

        /** Un ou plusieurs jours manques absorbes par des jokers. */
        SAVED_BY_FREEZE,

        /** Trop de jours manques : la serie repart de un. */
        RESET
    }

    public record Result(Streak streak, Outcome outcome, int freezesConsumed) {
    }

    /**
     * Enregistre une journee d'activite.
     *
     * <p>Le rechargement des jokers est fait avant l'evaluation : quelqu'un qui
     * revient apres une semaine doit beneficier du joker gagne entre-temps, pas
     * voir sa serie casser puis recevoir le joker juste apres.
     */
    public Result recordActivity(Streak streak, LocalDate today) {
        Streak refilled = refillFreezes(streak, today);

        if (refilled.isActiveOn(today)) {
            return new Result(refilled, Outcome.ALREADY_COUNTED, 0);
        }

        if (refilled.lastActiveOn() == null) {
            return new Result(advance(refilled, today, 1, 0), Outcome.STARTED, 0);
        }

        long gap = ChronoUnit.DAYS.between(refilled.lastActiveOn(), today);

        // Une date anterieure a la derniere activite : rattrapage hors ligne, deja
        // compte. On ne recule jamais une serie.
        if (gap <= 0) {
            return new Result(refilled, Outcome.ALREADY_COUNTED, 0);
        }

        if (gap == 1) {
            return new Result(advance(refilled, today, refilled.current() + 1, 0),
                    Outcome.CONTINUED, 0);
        }

        // Le premier jour du trou n'est pas "manque" : c'est le lendemain de la
        // derniere activite. Seuls les jours suivants le sont.
        int missedDays = (int) gap - 1;

        if (missedDays <= refilled.freezesAvailable()) {
            return new Result(advance(refilled, today, refilled.current() + 1, missedDays),
                    Outcome.SAVED_BY_FREEZE, missedDays);
        }

        // La serie repart a un, pas a zero : la journee d'aujourd'hui compte.
        return new Result(advance(refilled, today, 1, 0), Outcome.RESET, 0);
    }

    /**
     * Recharge les jokers une fois par semaine calendaire.
     *
     * <p>Le plafond est essentiel : sans lui, une absence de trois mois se
     * traduirait par une reserve suffisante pour tout absorber, et la serie ne
     * voudrait plus rien dire.
     */
    public Streak refillFreezes(Streak streak, LocalDate today) {
        if (streak.freezesRefilledOn() != null && sameWeek(streak.freezesRefilledOn(), today)) {
            return streak;
        }

        int refilled = Math.min(streak.freezesAvailable() + properties.freezesPerWeek(),
                properties.maxFreezes());

        return new Streak(streak.userId(), streak.current(), streak.longest(),
                streak.lastActiveOn(), refilled, streak.freezesUsedTotal(), today,
                Instant.now());
    }

    /**
     * Vrai si la serie se rompt faute d'activite aujourd'hui.
     *
     * <p>Sert a prevenir a temps. Prevenir quelqu'un que sa serie <em>est</em>
     * cassee ne sert a rien ; le prevenir qu'elle <em>va</em> l'etre lui laisse le
     * choix.
     */
    public boolean isAtRisk(Streak streak, LocalDate today) {
        if (streak.current() == 0 || streak.lastActiveOn() == null) {
            return false;
        }
        if (streak.isActiveOn(today)) {
            return false;
        }
        // A risque quand les jokers restants ne couvriront plus le trou demain.
        long gapTomorrow = ChronoUnit.DAYS.between(streak.lastActiveOn(), today.plusDays(1));
        return gapTomorrow - 1 > streak.freezesAvailable();
    }

    // ------------------------------------------------------------------

    private Streak advance(Streak streak, LocalDate today, int current, int freezesConsumed) {
        return new Streak(streak.userId(), current, Math.max(streak.longest(), current), today,
                streak.freezesAvailable() - freezesConsumed,
                streak.freezesUsedTotal() + freezesConsumed, streak.freezesRefilledOn(),
                Instant.now());
    }

    private boolean sameWeek(LocalDate first, LocalDate second) {
        WeekFields weekFields = WeekFields.of(Locale.FRANCE);
        return first.get(weekFields.weekBasedYear()) == second.get(weekFields.weekBasedYear())
                && first.get(weekFields.weekOfWeekBasedYear())
                        == second.get(weekFields.weekOfWeekBasedYear());
    }
}
