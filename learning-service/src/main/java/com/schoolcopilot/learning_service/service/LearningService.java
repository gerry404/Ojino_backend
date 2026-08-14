package com.schoolcopilot.learning_service.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.schoolcopilot.learning_service.client.ContentClient;
import com.schoolcopilot.learning_service.domain.LearningEvent;
import com.schoolcopilot.learning_service.domain.LearningEventType;
import com.schoolcopilot.learning_service.domain.MasteryLevel;
import com.schoolcopilot.learning_service.domain.NotionMastery;
import com.schoolcopilot.learning_service.exception.ApiException;
import com.schoolcopilot.learning_service.repository.LearningRepositories;

/**
 * Ce que l'eleve sait, et ce qu'il doit reprendre.
 *
 * <p>Deux responsabilites : enregistrer les faits d'apprentissage, et en deduire
 * un etat de maitrise. Les faits ne se modifient jamais ; la maitrise se
 * recalcule.
 */
@Service
public class LearningService {

    private static final Logger log = LoggerFactory.getLogger(LearningService.class);

    private final LearningRepositories.Events events;
    private final LearningRepositories.Mastery mastery;
    private final MasteryCalculator calculator;
    private final ContentClient content;

    public LearningService(LearningRepositories.Events events,
            LearningRepositories.Mastery mastery, MasteryCalculator calculator,
            ContentClient content) {
        this.events = events;
        this.mastery = mastery;
        this.calculator = calculator;
        this.content = content;
    }

    // ------------------------------------------------------------------
    // Enregistrement
    // ------------------------------------------------------------------

    /**
     * Enregistre un fait et met a jour la maitrise de la notion concernee.
     *
     * <p>Le recalcul ne porte que sur cette notion : recalculer tout le systeme a
     * chaque exercice couterait cher pour rien, puisqu'un evenement ne peut en
     * affecter qu'une.
     */
    public NotionMastery record(String userId, LearningEvent draft) {
        if (draft.score() < 0 || draft.score() > 1) {
            throw ApiException.invalidScore();
        }

        LearningEvent event = new LearningEvent(null, userId, draft.systemCode(),
                draft.notionCode(), draft.type(), draft.score(),
                draft.weight() <= 0 ? 1 : draft.weight(), draft.sourceCode(),
                draft.durationSeconds(),
                draft.occurredAt() == null ? Instant.now() : draft.occurredAt());

        events.save(event);
        return recompute(userId, event.systemCode(), event.notionCode());
    }

    /** Recalcule integralement la maitrise d'une notion depuis ses evenements. */
    public NotionMastery recompute(String userId, String systemCode, String notionCode) {
        List<LearningEvent> history =
                events.findByUserIdAndSystemCodeAndNotionCode(userId, systemCode, notionCode);

        MasteryCalculator.Result result = calculator.compute(history);
        Instant lastEventAt = history.stream()
                .map(LearningEvent::occurredAt)
                .max(Instant::compareTo)
                .orElse(null);

        NotionMastery updated = new NotionMastery(
                NotionMastery.idFor(userId, systemCode, notionCode), userId, systemCode,
                notionCode, result.score(), result.attempts(), result.level(), lastEventAt,
                Instant.now());

        log.debug("Maitrise de {} sur {} : {} ({})", userId, notionCode, result.level(),
                result.score());
        return mastery.save(updated);
    }

    // ------------------------------------------------------------------
    // Lecture
    // ------------------------------------------------------------------

    public List<NotionMastery> masteryFor(String userId, String systemCode) {
        return mastery.findByUserIdAndSystemCode(userId, systemCode);
    }

    public NotionMastery masteryOf(String userId, String systemCode, String notionCode) {
        return mastery.findByUserIdAndSystemCodeAndNotionCode(userId, systemCode, notionCode)
                .orElseThrow(() -> ApiException.noMastery(notionCode));
    }

    /** Les notions ou l'eleve bloque : le point de depart de toute remediation. */
    public List<NotionMastery> gaps(String userId, String systemCode) {
        return mastery.findByUserIdAndSystemCodeAndLevelIn(userId, systemCode,
                List.of(MasteryLevel.STRUGGLING));
    }

    /**
     * Ce qu'il faut reprendre pour debloquer une notion.
     *
     * <p>C'est le croisement des deux moities du probleme : content-service dit ce
     * qui precede la notion dans le programme, ce service dit ce qui est deja
     * acquis. Le resultat est la difference — et c'est tout l'interet d'avoir
     * separe le graphe du suivi.
     *
     * <p>Un eleve qui bloque sur les derivees se voit proposer les limites, pas
     * tout le chapitre depuis le debut.
     */
    public List<Remediation> remediationFor(String userId, String systemCode, String notionCode) {
        List<ContentClient.NotionView> path = content.learningPath(systemCode, notionCode);

        Map<String, NotionMastery> known = masteryFor(userId, systemCode).stream()
                .collect(java.util.stream.Collectors.toMap(NotionMastery::notionCode,
                        Function.identity(), (first, second) -> first));

        return path.stream()
                .map(notion -> new Remediation(notion, known.get(notion.code())))
                .filter(Remediation::needsWork)
                .toList();
    }

    /**
     * Une etape de remediation : la notion a reprendre et ce qu'on en sait.
     *
     * @param mastery null si la notion n'a jamais ete evaluee — c'est alors une
     *        lacune probable, et elle doit figurer dans le parcours
     */
    public record Remediation(ContentClient.NotionView notion, NotionMastery mastery) {

        private static final Set<MasteryLevel> DONE = Set.of(MasteryLevel.MASTERED);

        public boolean needsWork() {
            return mastery == null || !DONE.contains(mastery.level());
        }

        public MasteryLevel level() {
            return mastery == null ? MasteryLevel.UNKNOWN : mastery.level();
        }
    }

    /** Les derniers faits enregistres, pour un fil d'activite. */
    public List<LearningEvent> recentActivity(String userId) {
        return events.findTop50ByUserIdOrderByOccurredAtDesc(userId);
    }

    /** Repartition par palier, pour un tableau de bord. */
    public Map<MasteryLevel, Long> distribution(String userId, String systemCode) {
        return masteryFor(userId, systemCode).stream()
                .collect(java.util.stream.Collectors.groupingBy(NotionMastery::level,
                        java.util.stream.Collectors.counting()));
    }

    /** Utilitaire de construction pour les appelants qui n'ont que les champs bruts. */
    public static LearningEvent draft(String systemCode, String notionCode,
            LearningEventType type, double score, double weight, String sourceCode,
            Integer durationSeconds, Instant occurredAt) {
        return new LearningEvent(null, null, systemCode, notionCode, type, score, weight,
                sourceCode, durationSeconds, occurredAt);
    }
}
