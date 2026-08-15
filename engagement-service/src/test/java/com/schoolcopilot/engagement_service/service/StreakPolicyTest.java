package com.schoolcopilot.engagement_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schoolcopilot.engagement_service.config.EngagementProperties;
import com.schoolcopilot.engagement_service.domain.Streak;

/**
 * La mecanique la plus sensible du service.
 *
 * <p>Mal reglee, elle decourage au lieu d'encourager — et le mal est fait avant
 * qu'on s'en apercoive. Ces tests fixent surtout ce qui <em>ne doit pas</em>
 * arriver : une serie remise a zero pour une soiree manquee.
 *
 * <p>Reglages : deux jokers au depart, trois au maximum, un par semaine.
 */
class StreakPolicyTest {

    private final StreakPolicy policy = new StreakPolicy(
            new EngagementProperties(2, 3, 1, 19, 7, 2.5, 4.0));

    /** Un lundi, pour que les semaines calendaires soient lisibles. */
    private final LocalDate monday = LocalDate.of(2026, 8, 17);

    @Test
    @DisplayName("la premiere activite demarre la serie a un")
    void firstActivityStartsAtOne() {
        StreakPolicy.Result result = policy.recordActivity(fresh(), monday);

        assertThat(result.outcome()).isEqualTo(StreakPolicy.Outcome.STARTED);
        assertThat(result.streak().current()).isEqualTo(1);
    }

    @Test
    @DisplayName("un second passage le meme jour ne fait rien avancer")
    void sameDayIsIdempotent() {
        Streak afterFirst = policy.recordActivity(fresh(), monday).streak();

        StreakPolicy.Result again = policy.recordActivity(afterFirst, monday);

        assertThat(again.outcome()).isEqualTo(StreakPolicy.Outcome.ALREADY_COUNTED);
        assertThat(again.streak().current()).isEqualTo(1);
    }

    @Test
    @DisplayName("un jour consecutif fait avancer la serie")
    void consecutiveDayContinues() {
        Streak day1 = policy.recordActivity(fresh(), monday).streak();

        StreakPolicy.Result day2 = policy.recordActivity(day1, monday.plusDays(1));

        assertThat(day2.outcome()).isEqualTo(StreakPolicy.Outcome.CONTINUED);
        assertThat(day2.streak().current()).isEqualTo(2);
    }

    @Test
    @DisplayName("un jour manque consomme un joker au lieu de casser la serie")
    void oneMissedDayIsAbsorbedByAFreeze() {
        Streak established = streakOf(10, monday, 2);

        // Le mardi est saute, l'eleve revient le mercredi.
        StreakPolicy.Result result = policy.recordActivity(established, monday.plusDays(2));

        // C'est tout l'enjeu : dix jours d'efforts ne disparaissent pas pour une
        // soiree manquee.
        assertThat(result.outcome()).isEqualTo(StreakPolicy.Outcome.SAVED_BY_FREEZE);
        assertThat(result.streak().current()).isEqualTo(11);
        assertThat(result.freezesConsumed()).isEqualTo(1);
        assertThat(result.streak().freezesAvailable()).isEqualTo(1);
    }

    @Test
    @DisplayName("deux jours manques consomment deux jokers")
    void twoMissedDaysConsumeTwoFreezes() {
        Streak established = streakOf(10, monday, 2);

        StreakPolicy.Result result = policy.recordActivity(established, monday.plusDays(3));

        assertThat(result.outcome()).isEqualTo(StreakPolicy.Outcome.SAVED_BY_FREEZE);
        assertThat(result.streak().freezesAvailable()).isZero();
    }

    @Test
    @DisplayName("sans joker suffisant, la serie repart a un et non a zero")
    void withoutEnoughFreezesTheStreakRestartsAtOne() {
        Streak established = streakOf(10, monday, 0);

        StreakPolicy.Result result = policy.recordActivity(established, monday.plusDays(5));

        assertThat(result.outcome()).isEqualTo(StreakPolicy.Outcome.RESET);
        // La journee d'aujourd'hui compte : repartir a zero effacerait l'effort du
        // jour meme ou l'eleve revient.
        assertThat(result.streak().current()).isEqualTo(1);
    }

    @Test
    @DisplayName("le record survit a une serie cassee")
    void longestSurvivesAReset() {
        Streak established = streakOf(40, monday, 0);

        StreakPolicy.Result result = policy.recordActivity(established, monday.plusDays(10));

        // L'eleve garde la trace de ce dont il a ete capable.
        assertThat(result.streak().current()).isEqualTo(1);
        assertThat(result.streak().longest()).isEqualTo(40);
    }

    @Test
    @DisplayName("les jokers se rechargent une fois par semaine, sans depasser le plafond")
    void freezesRefillWeeklyUpToTheCap() {
        Streak spent = new Streak("u", 5, 5, monday, 0, 2, monday, null);

        Streak nextWeek = policy.refillFreezes(spent, monday.plusDays(7));
        assertThat(nextWeek.freezesAvailable()).isEqualTo(1);

        Streak weekAfter = policy.refillFreezes(nextWeek, monday.plusDays(14));
        assertThat(weekAfter.freezesAvailable()).isEqualTo(2);
    }

    @Test
    @DisplayName("le rechargement ne se produit pas deux fois la meme semaine")
    void refillHappensOncePerWeek() {
        Streak spent = new Streak("u", 5, 5, monday, 0, 2, monday, null);

        Streak sameWeek = policy.refillFreezes(spent, monday.plusDays(3));

        assertThat(sameWeek.freezesAvailable()).isZero();
    }

    @Test
    @DisplayName("le plafond empeche d'accumuler des jokers pendant une longue absence")
    void freezesAreCapped() {
        Streak hoarding = new Streak("u", 5, 5, monday, 3, 0, monday, null);

        Streak later = policy.refillFreezes(hoarding, monday.plusDays(28));

        // Sans plafond, une absence de trois mois donnerait de quoi tout absorber,
        // et la serie ne voudrait plus rien dire.
        assertThat(later.freezesAvailable()).isEqualTo(3);
    }

    @Test
    @DisplayName("le rechargement precede l'evaluation")
    void refillHappensBeforeEvaluation() {
        // Serie active lundi, sans joker, rechargement date de la semaine passee.
        Streak spent = new Streak("u", 10, 10, monday, 0, 2, monday.minusDays(7), null);

        StreakPolicy.Result result = policy.recordActivity(spent, monday.plusDays(2));

        // Le joker gagne entre-temps doit servir, plutot que d'arriver juste apres
        // la rupture.
        assertThat(result.outcome()).isEqualTo(StreakPolicy.Outcome.SAVED_BY_FREEZE);
    }

    @Test
    @DisplayName("une date anterieure ne fait jamais reculer la serie")
    void backdatedActivityNeverRegresses() {
        Streak established = streakOf(10, monday.plusDays(5), 2);

        StreakPolicy.Result result = policy.recordActivity(established, monday);

        assertThat(result.outcome()).isEqualTo(StreakPolicy.Outcome.ALREADY_COUNTED);
        assertThat(result.streak().current()).isEqualTo(10);
    }

    // ------------------------------------------------------------------
    // Alerte
    // ------------------------------------------------------------------

    @Test
    @DisplayName("une serie sans joker est a risque des le lendemain")
    void streakWithoutFreezesIsAtRisk() {
        Streak fragile = streakOf(10, monday, 0);

        assertThat(policy.isAtRisk(fragile, monday.plusDays(1))).isTrue();
    }

    @Test
    @DisplayName("une serie avec joker n'est pas encore a risque")
    void streakWithFreezesIsSafe() {
        Streak protectedStreak = streakOf(10, monday, 2);

        assertThat(policy.isAtRisk(protectedStreak, monday.plusDays(1))).isFalse();
    }

    @Test
    @DisplayName("une serie deja active aujourd'hui n'est pas a risque")
    void activeTodayIsNotAtRisk() {
        assertThat(policy.isAtRisk(streakOf(10, monday, 0), monday)).isFalse();
    }

    @Test
    @DisplayName("une serie inexistante ne declenche aucune alerte")
    void noStreakNoAlert() {
        // Relancer quelqu'un sur une serie qu'il n'a pas serait absurde.
        assertThat(policy.isAtRisk(fresh(), monday)).isFalse();
    }

    private Streak fresh() {
        return Streak.start("u", 2);
    }

    private Streak streakOf(int current, LocalDate lastActive, int freezes) {
        return new Streak("u", current, current, lastActive, freezes, 0, lastActive, null);
    }
}
