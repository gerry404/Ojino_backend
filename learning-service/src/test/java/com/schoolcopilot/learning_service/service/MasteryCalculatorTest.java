package com.schoolcopilot.learning_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schoolcopilot.learning_service.config.MasteryProperties;
import com.schoolcopilot.learning_service.domain.LearningEvent;
import com.schoolcopilot.learning_service.domain.LearningEventType;
import com.schoolcopilot.learning_service.domain.MasteryLevel;

/**
 * Le calcul de maitrise est de l'arithmetique pure : il se verifie sans base ni
 * Spring. C'est aussi la partie qu'on ajustera le plus souvent, donc celle qui a
 * le plus besoin d'un filet.
 */
class MasteryCalculatorTest {

    private final MasteryCalculator calculator = new MasteryCalculator(
            new MasteryProperties(0.80, 0.40, 3, 0.30));

    private final Instant now = Instant.now();

    @Test
    @DisplayName("sans aucun resultat, la notion est inconnue et non pas ratee")
    void noEventsMeansUnknown() {
        MasteryCalculator.Result result = calculator.compute(List.of());

        // Nuance importante : ne pas savoir n'est pas echouer. Un score de zero
        // ferait remonter toutes les notions jamais vues comme des difficultes.
        assertThat(result.level()).isEqualTo(MasteryLevel.UNKNOWN);
        assertThat(result.attempts()).isZero();
    }

    @Test
    @DisplayName("consulter une lecon ne compte pas comme un resultat")
    void viewingAResourceIsNotGraded() {
        MasteryCalculator.Result result = calculator.compute(List.of(
                event(LearningEventType.RESOURCE_VIEWED, 1.0, 1, 0),
                event(LearningEventType.RESOURCE_VIEWED, 1.0, 1, 1)));

        // Sinon il suffirait de faire defiler les pages pour tout maitriser.
        assertThat(result.level()).isEqualTo(MasteryLevel.UNKNOWN);
        assertThat(result.attempts()).isZero();
    }

    @Test
    @DisplayName("en dessous du minimum d'essais, on ne conclut pas")
    void tooFewAttemptsStaysLearning() {
        MasteryCalculator.Result result = calculator.compute(List.of(
                graded(1.0, 1, 0),
                graded(1.0, 1, 1)));

        // Deux reussites ne font pas une notion acquise : ce serait declarer
        // maitrise sur un coup de chance.
        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.level()).isEqualTo(MasteryLevel.LEARNING);
    }

    @Test
    @DisplayName("des reussites repetees rendent la notion acquise")
    void repeatedSuccessMastersTheNotion() {
        MasteryCalculator.Result result = calculator.compute(List.of(
                graded(1.0, 1, 0), graded(1.0, 1, 1), graded(1.0, 1, 2)));

        assertThat(result.level()).isEqualTo(MasteryLevel.MASTERED);
        assertThat(result.attempts()).isEqualTo(3);
    }

    @Test
    @DisplayName("des echecs repetes declenchent la remediation")
    void repeatedFailureFlagsStruggling() {
        MasteryCalculator.Result result = calculator.compute(List.of(
                graded(0.2, 1, 0), graded(0.1, 1, 1), graded(0.0, 1, 2)));

        assertThat(result.level()).isEqualTo(MasteryLevel.STRUGGLING);
    }

    @Test
    @DisplayName("le dernier resultat pese plus que les anciens")
    void recentResultWeighsMore() {
        // Deux echecs anciens, une reussite recente : quelqu'un qui a compris.
        MasteryCalculator.Result withRecovery = calculator.compute(List.of(
                graded(0.0, 1, 0), graded(0.0, 1, 1), graded(1.0, 1, 5)));

        // Meme repartition, mais la reussite est la plus ancienne.
        MasteryCalculator.Result withDecline = calculator.compute(List.of(
                graded(1.0, 1, 0), graded(0.0, 1, 1), graded(0.0, 1, 5)));

        assertThat(withRecovery.score()).isGreaterThan(withDecline.score());
    }

    @Test
    @DisplayName("un devoir surveille pese plus qu'un exercice d'entrainement")
    void weightIsHonoured() {
        MasteryCalculator.Result result = calculator.compute(List.of(
                graded(0.0, 5, 0),
                graded(1.0, 1, 1),
                graded(1.0, 1, 2)));

        // La moyenne simple donnerait 0,67. Avec un poids de 5 sur l'echec, elle
        // tombe a 0,29 — et le dernier resultat la remonte un peu.
        assertThat(result.score()).isLessThan(0.6);
    }

    @Test
    @DisplayName("des poids tous nuls ne provoquent pas de division par zero")
    void zeroWeightsFallBackToASimpleMean() {
        MasteryCalculator.Result result = calculator.compute(List.of(
                graded(1.0, 0, 0), graded(0.0, 0, 1), graded(1.0, 0, 2)));

        assertThat(result.score()).isBetween(0.0, 1.0);
    }

    @Test
    @DisplayName("le score reste borne entre 0 et 1")
    void scoreStaysWithinBounds() {
        MasteryCalculator.Result result = calculator.compute(List.of(
                graded(1.0, 100, 0), graded(1.0, 100, 1), graded(1.0, 100, 2)));

        assertThat(result.score()).isBetween(0.0, 1.0);
    }

    private LearningEvent graded(double score, double weight, int hoursAgo) {
        return event(LearningEventType.EXERCISE_ATTEMPTED, score, weight, hoursAgo);
    }

    private LearningEvent event(LearningEventType type, double score, double weight,
            int hoursAgo) {
        return new LearningEvent(null, "user-1", "CM-FR", "LIMITES", type, score, weight, null,
                null, now.minus(10 - hoursAgo, ChronoUnit.HOURS));
    }
}
