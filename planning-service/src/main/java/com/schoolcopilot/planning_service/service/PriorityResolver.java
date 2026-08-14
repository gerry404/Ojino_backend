package com.schoolcopilot.planning_service.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.schoolcopilot.planning_service.client.LearningClient;
import com.schoolcopilot.planning_service.config.PlanningProperties;
import com.schoolcopilot.planning_service.domain.Deadline;
import com.schoolcopilot.planning_service.domain.SessionReason;

/**
 * Decide de ce qu'il faut travailler, et dans quel ordre.
 *
 * <p>Trois sources se disputent le temps de l'eleve : les echeances qui
 * approchent, les notions ou il bloque, et celles qu'il maitrise mais qu'il va
 * oublier. Ce composant les ramene a une echelle commune pour pouvoir les
 * comparer.
 *
 * <p>Logique pure, sans base ni reseau : les poids relatifs sont ce qui se
 * discutera le plus, autant qu'ils se lisent d'un coup d'oeil.
 */
@Component
public class PriorityResolver {

    /**
     * Une notion en difficulte pese plus qu'une revision d'entretien, mais moins
     * qu'un examen de demain. Ces trois constantes fixent l'echelle ; l'urgence
     * d'une echeance, elle, se calcule.
     */
    private static final double STRUGGLING_WEIGHT = 3.0;
    private static final double SPACED_REVIEW_WEIGHT = 1.0;
    private static final double DEADLINE_SCALE = 5.0;

    private final PlanningProperties properties;

    public PriorityResolver(PlanningProperties properties) {
        this.properties = properties;
    }

    /**
     * Assemble les priorites de la semaine.
     *
     * <p>Une meme notion peut remonter de plusieurs sources — bloquer sur les
     * limites <em>et</em> avoir un controle dessus. Elle n'apparait alors qu'une
     * fois, avec le poids le plus fort : la dupliquer remplirait le planning d'une
     * seule notion.
     */
    public List<StudyPriority> resolve(LocalDate today, List<Deadline> deadlines,
            List<LearningClient.MasteryView> gaps, List<LearningClient.MasteryView> mastery) {

        Map<String, StudyPriority> byNotion = new LinkedHashMap<>();

        fromDeadlines(today, deadlines).forEach(priority -> merge(byNotion, priority));
        fromGaps(gaps).forEach(priority -> merge(byNotion, priority));
        fromSpacedReview(mastery).forEach(priority -> merge(byNotion, priority));

        return byNotion.values().stream()
                .sorted(Comparator.comparingDouble(StudyPriority::weight).reversed())
                .toList();
    }

    // ------------------------------------------------------------------

    /**
     * Une echeance ne pese qu'a partir du moment ou il est raisonnable de s'y
     * mettre : un examen officiel dans six mois ne doit pas occuper le planning de
     * cette semaine.
     */
    private List<StudyPriority> fromDeadlines(LocalDate today, List<Deadline> deadlines) {
        List<StudyPriority> priorities = new ArrayList<>();

        deadlines.stream()
                .filter(deadline -> deadline.isUpcoming(today))
                .filter(deadline -> deadline.daysUntil(today) <= deadline.type().preparationDays())
                .forEach(deadline -> {
                    double weight = DEADLINE_SCALE * deadline.urgency(today);
                    deadline.notionCodes().forEach(notion ->
                            priorities.add(StudyPriority.forDeadline(notion, weight, deadline.id())));
                });

        return priorities;
    }

    /** Ce sur quoi l'eleve bloque : la remediation passe avant la progression. */
    private List<StudyPriority> fromGaps(List<LearningClient.MasteryView> gaps) {
        return gaps.stream()
                .map(gap -> StudyPriority.of(gap.notionCode(), SessionReason.REMEDIATION,
                        // Plus le score est bas, plus la notion remonte.
                        STRUGGLING_WEIGHT * (1 - gap.score())))
                .toList();
    }

    /**
     * Les notions acquises depuis assez longtemps pour meriter un entretien.
     *
     * <p>Sans cela, tout ce qui est appris en septembre est oublie en juin, et
     * l'eleve ne s'en apercoit qu'a l'examen.
     */
    private List<StudyPriority> fromSpacedReview(List<LearningClient.MasteryView> mastery) {
        Instant threshold = Instant.now().minus(properties.spacedReviewDays(), ChronoUnit.DAYS);

        return mastery.stream()
                .filter(LearningClient.MasteryView::isMastered)
                .filter(view -> view.lastEventAt() != null && view.lastEventAt().isBefore(threshold))
                .map(view -> StudyPriority.of(view.notionCode(), SessionReason.SPACED_REVIEW,
                        SPACED_REVIEW_WEIGHT))
                .toList();
    }

    /** Garde le poids le plus fort quand une notion remonte de plusieurs sources. */
    private void merge(Map<String, StudyPriority> byNotion, StudyPriority candidate) {
        byNotion.merge(candidate.notionCode(), candidate,
                (existing, incoming) -> incoming.weight() > existing.weight() ? incoming : existing);
    }
}
