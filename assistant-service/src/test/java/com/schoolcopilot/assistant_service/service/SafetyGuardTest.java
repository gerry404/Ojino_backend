package com.schoolcopilot.assistant_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schoolcopilot.assistant_service.domain.LanguageRegister;

import tools.jackson.databind.json.JsonMapper;

/**
 * Les garde-fous. Le public commence a la maternelle, et ces regles sont ce qui
 * separe l'assistant d'un agent conversationnel ordinaire.
 */
class SafetyGuardTest {

    private final SafetyGuard guard = new SafetyGuard(JsonMapper.builder().build());

    @Test
    @DisplayName("une question scolaire ordinaire passe")
    void ordinaryQuestionIsAllowed() {
        assertThat(guard.inspect("comment calculer une limite ?", "fr"))
                .isInstanceOf(SafetyGuard.Decision.Allow.class);
    }

    @Test
    @DisplayName("une expression de detresse est interceptee, jamais envoyee au modele")
    void distressIsIntercepted() {
        SafetyGuard.Decision decision = guard.inspect("je veux mourir", "fr");

        // Laisser un modele improviser la-dessus serait irresponsable : la reponse
        // est ecrite a l'avance et relue.
        assertThat(decision).isInstanceOf(SafetyGuard.Decision.Intercept.class);
        assertThat(((SafetyGuard.Decision.Intercept) decision).reply())
                .contains("adulte");
    }

    @Test
    @DisplayName("la detresse est detectee malgre les accents et la casse")
    void distressDetectionIgnoresAccentsAndCase() {
        // Sans normalisation, "Suicidé" passerait a cote du declencheur.
        assertThat(guard.inspect("je pense au SUICIDE", "fr"))
                .isInstanceOf(SafetyGuard.Decision.Intercept.class);
    }

    @Test
    @DisplayName("l'orientation existe dans les deux langues")
    void distressReplyExistsInBothLanguages() {
        SafetyGuard.Decision french = guard.inspect("je veux mourir", "fr");
        SafetyGuard.Decision english = guard.inspect("i want to kill myself", "en");

        assertThat(((SafetyGuard.Decision.Intercept) french).reply()).isNotBlank();
        assertThat(((SafetyGuard.Decision.Intercept) english).reply())
                .isNotBlank()
                .isNotEqualTo(((SafetyGuard.Decision.Intercept) french).reply());
    }

    @Test
    @DisplayName("une langue inconnue retombe sur le francais plutot que d'echouer")
    void unknownLanguageFallsBack() {
        SafetyGuard.Decision decision = guard.inspect("je veux mourir", "de");

        assertThat(((SafetyGuard.Decision.Intercept) decision).reply()).isNotBlank();
    }

    @Test
    @DisplayName("demander de faire le devoir n'est pas refuse, mais encadre")
    void homeworkRequestIsGuidedNotRefused() {
        SafetyGuard.Decision decision = guard.inspect("fais mon devoir de maths", "fr");

        // Refuser tout court pousserait l'eleve vers un autre outil qui, lui,
        // ecrira le devoir.
        assertThat(decision).isInstanceOf(SafetyGuard.Decision.AllowWithGuidance.class);
        assertThat(((SafetyGuard.Decision.AllowWithGuidance) decision).guidance())
                .containsIgnoringCase("guider");
    }

    @Test
    @DisplayName("les consignes generales accompagnent chaque appel")
    void standingGuidanceCoversEveryRule() {
        // Ce que l'assistant ne fait pas ne depend pas de la formulation de la
        // question : toutes les regles partent a chaque fois.
        assertThat(guard.standingGuidance()).hasSize(3).allSatisfy(guidance ->
                assertThat(guidance).isNotBlank());
    }

    @Test
    @DisplayName("une reponse trop longue pour le registre est raccourcie")
    void overlongReplyIsTruncated() {
        String verbose = "Une phrase. ".repeat(200);

        String result = guard.enforceReplyLength(verbose, LanguageRegister.EARLY_YEARS);

        assertThat(result.length())
                .isLessThanOrEqualTo(LanguageRegister.EARLY_YEARS.maxReplyChars());
    }

    @Test
    @DisplayName("la coupe tombe sur une fin de phrase")
    void truncationEndsOnASentence() {
        String verbose = "Une phrase complete. ".repeat(200);

        String result = guard.enforceReplyLength(verbose, LanguageRegister.EARLY_YEARS);

        // Une reponse coupee au milieu d'un mot est plus deroutante qu'une reponse
        // plus courte.
        assertThat(result).endsWith(".");
    }

    @Test
    @DisplayName("une reponse deja courte n'est pas touchee")
    void shortReplyIsLeftAlone() {
        String reply = "Une limite, c'est la valeur vers laquelle on tend.";

        assertThat(guard.enforceReplyLength(reply, LanguageRegister.EARLY_YEARS))
                .isEqualTo(reply);
    }

    @Test
    @DisplayName("un lyceen a droit a une reponse plus longue qu'un enfant")
    void lengthLimitFollowsTheRegister() {
        String reply = "Une phrase. ".repeat(100);

        String forChild = guard.enforceReplyLength(reply, LanguageRegister.EARLY_YEARS);
        String forTeen = guard.enforceReplyLength(reply, LanguageRegister.HIGH_SCHOOL);

        assertThat(forChild.length()).isLessThan(forTeen.length());
    }
}
