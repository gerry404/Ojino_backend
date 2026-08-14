package com.schoolcopilot.learning_service.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.schoolcopilot.learning_service.config.MasteryProperties;
import com.schoolcopilot.learning_service.domain.LearningEvent;
import com.schoolcopilot.learning_service.domain.MasteryLevel;

/**
 * Le calcul de la maitrise a partir des evenements.
 *
 * <p>Isole du service : c'est de l'arithmetique pure, elle se lit et se verifie
 * sans base ni Spring. C'est aussi la partie qu'on ajustera le plus souvent, et
 * on veut pouvoir le faire sans toucher au reste.
 *
 * <p>Deux idees seulement :
 * <ul>
 *   <li>une moyenne <em>ponderee</em> — un devoir surveille pese plus qu'un
 *       exercice d'entrainement ;</li>
 *   <li>un poids supplementaire au dernier resultat — quelqu'un qui a rate en
 *       septembre et reussit en juin a compris, et son score doit le dire.</li>
 * </ul>
 */
@Component
public class MasteryCalculator {

    private final MasteryProperties properties;

    public MasteryCalculator(MasteryProperties properties) {
        this.properties = properties;
    }

    /** Le score et le palier deduits d'une suite d'evenements. */
    public record Result(double score, int attempts, MasteryLevel level) {
    }

    /**
     * @param events dans n'importe quel ordre ; le plus recent est identifie par
     *        sa date, pas par sa position
     */
    public Result compute(List<LearningEvent> events) {
        List<LearningEvent> graded = events.stream()
                .filter(LearningEvent::isGraded)
                .toList();

        if (graded.isEmpty()) {
            return new Result(0, 0, MasteryLevel.UNKNOWN);
        }

        double score = blend(weightedMean(graded), mostRecentScore(graded));
        return new Result(round(score), graded.size(), levelOf(score, graded.size()));
    }

    // ------------------------------------------------------------------

    private double weightedMean(List<LearningEvent> graded) {
        double totalWeight = graded.stream()
                .mapToDouble(event -> Math.max(event.weight(), 0))
                .sum();

        // Des poids tous nuls ne doivent pas produire une division par zero :
        // on retombe alors sur une moyenne simple.
        if (totalWeight <= 0) {
            return graded.stream().mapToDouble(LearningEvent::score).average().orElse(0);
        }

        double weighted = graded.stream()
                .mapToDouble(event -> event.score() * Math.max(event.weight(), 0))
                .sum();
        return weighted / totalWeight;
    }

    private double mostRecentScore(List<LearningEvent> graded) {
        return graded.stream()
                .max(java.util.Comparator.comparing(LearningEvent::occurredAt))
                .map(LearningEvent::score)
                .orElse(0.0);
    }

    private double blend(double mean, double latest) {
        double recency = clamp(properties.recencyWeight());
        return clamp((1 - recency) * mean + recency * latest);
    }

    /**
     * En dessous de {@code minAttempts}, on refuse de conclure : ni acquise sur un
     * coup de chance, ni en difficulte sur un seul faux pas.
     */
    private MasteryLevel levelOf(double score, int attempts) {
        if (attempts < properties.minAttempts()) {
            return MasteryLevel.LEARNING;
        }
        if (score >= properties.masteredThreshold()) {
            return MasteryLevel.MASTERED;
        }
        if (score < properties.strugglingThreshold()) {
            return MasteryLevel.STRUGGLING;
        }
        return MasteryLevel.LEARNING;
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private double round(double value) {
        return Math.round(value * 1000) / 1000.0;
    }
}
