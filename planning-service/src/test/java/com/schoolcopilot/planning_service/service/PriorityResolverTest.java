package com.schoolcopilot.planning_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schoolcopilot.planning_service.client.LearningClient;
import com.schoolcopilot.planning_service.config.PlanningProperties;
import com.schoolcopilot.planning_service.domain.Deadline;
import com.schoolcopilot.planning_service.domain.DeadlineType;
import com.schoolcopilot.planning_service.domain.SessionReason;

/**
 * L'arbitrage entre echeances, lacunes et revisions espacees.
 *
 * <p>C'est la partie qui se discutera le plus : ces tests fixent les
 * comportements attendus, pas les valeurs exactes des poids.
 */
class PriorityResolverTest {

    private final PriorityResolver resolver = new PriorityResolver(
            new PlanningProperties(45, 20, 10, 4, 14));

    private final LocalDate today = LocalDate.of(2026, 8, 17);

    @Test
    @DisplayName("un controle proche passe devant une notion en difficulte")
    void nearDeadlineOutranksAGap() {
        List<StudyPriority> priorities = resolver.resolve(today,
                List.of(deadline("D1", DeadlineType.EXAM, 5, today.plusDays(1), "DERIVEES")),
                List.of(gap("LIMITES", 0.1)),
                List.of());

        assertThat(priorities.get(0).notionCode()).isEqualTo("DERIVEES");
        assertThat(priorities.get(0).reason()).isEqualTo(SessionReason.DEADLINE);
    }

    @Test
    @DisplayName("une echeance encore lointaine n'occupe pas le planning")
    void distantDeadlineIsIgnored() {
        List<StudyPriority> priorities = resolver.resolve(today,
                // Un controle se prepare dix jours avant, pas six mois.
                List.of(deadline("D1", DeadlineType.EXAM, 5, today.plusMonths(6), "DERIVEES")),
                List.of(), List.of());

        assertThat(priorities).isEmpty();
    }

    @Test
    @DisplayName("un examen officiel se prepare bien plus tot qu'une interrogation")
    void officialExamStartsEarlier() {
        LocalDate inThreeWeeks = today.plusDays(21);

        assertThat(resolver.resolve(today,
                List.of(deadline("D1", DeadlineType.OFFICIAL_EXAM, 5, inThreeWeeks, "DERIVEES")),
                List.of(), List.of())).isNotEmpty();

        assertThat(resolver.resolve(today,
                List.of(deadline("D2", DeadlineType.QUIZ, 5, inThreeWeeks, "DERIVEES")),
                List.of(), List.of())).isEmpty();
    }

    @Test
    @DisplayName("une echeance passee ne pese plus")
    void pastDeadlineIsIgnored() {
        assertThat(resolver.resolve(today,
                List.of(deadline("D1", DeadlineType.EXAM, 5, today.minusDays(1), "DERIVEES")),
                List.of(), List.of())).isEmpty();
    }

    @Test
    @DisplayName("plus le score est bas, plus la notion remonte")
    void lowerScoreRanksHigher() {
        List<StudyPriority> priorities = resolver.resolve(today, List.of(),
                List.of(gap("PRESQUE", 0.35), gap("PERDU", 0.05)),
                List.of());

        assertThat(priorities.get(0).notionCode()).isEqualTo("PERDU");
    }

    @Test
    @DisplayName("une notion acquise depuis longtemps revient en entretien")
    void staleMasteryComesBackForReview() {
        List<StudyPriority> priorities = resolver.resolve(today, List.of(), List.of(),
                List.of(mastered("LIMITES", 40)));

        // Sans cela, ce qui est appris en septembre est oublie en juin.
        assertThat(priorities).hasSize(1);
        assertThat(priorities.get(0).reason()).isEqualTo(SessionReason.SPACED_REVIEW);
    }

    @Test
    @DisplayName("une notion acquise recemment n'est pas reproposee")
    void freshMasteryIsLeftAlone() {
        assertThat(resolver.resolve(today, List.of(), List.of(),
                List.of(mastered("LIMITES", 2)))).isEmpty();
    }

    @Test
    @DisplayName("une notion qui remonte de deux sources n'apparait qu'une fois")
    void aNotionAppearsOnlyOnce() {
        List<StudyPriority> priorities = resolver.resolve(today,
                List.of(deadline("D1", DeadlineType.EXAM, 5, today.plusDays(2), "LIMITES")),
                List.of(gap("LIMITES", 0.1)),
                List.of());

        // La dupliquer remplirait le planning d'une seule notion.
        assertThat(priorities).hasSize(1);
        assertThat(priorities.get(0).reason()).isEqualTo(SessionReason.DEADLINE);
    }

    @Test
    @DisplayName("une echeance sans notion precise ne genere aucune priorite ciblee")
    void deadlineWithoutNotionsProducesNothing() {
        Deadline broad = new Deadline("D1", "user-1", "CM-FR", DeadlineType.EXAM, "Controle",
                "MATH", List.of(), today.plusDays(2), 5, false, Instant.now());

        assertThat(resolver.resolve(today, List.of(broad), List.of(), List.of())).isEmpty();
    }

    private Deadline deadline(String id, DeadlineType type, int importance, LocalDate dueOn,
            String... notions) {
        return new Deadline(id, "user-1", "CM-FR", type, "Echeance", "MATH", List.of(notions),
                dueOn, importance, false, Instant.now());
    }

    private LearningClient.MasteryView gap(String notionCode, double score) {
        return new LearningClient.MasteryView(notionCode, "STRUGGLING", score, 3, Instant.now());
    }

    private LearningClient.MasteryView mastered(String notionCode, int daysAgo) {
        return new LearningClient.MasteryView(notionCode, "MASTERED", 0.9, 5,
                Instant.now().minus(daysAgo, ChronoUnit.DAYS));
    }
}
