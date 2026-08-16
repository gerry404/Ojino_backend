package com.schoolcopilot.assistant_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schoolcopilot.assistant_service.config.AssistantProperties;
import com.schoolcopilot.assistant_service.domain.UsageQuota;

/**
 * Les quotas arbitrent un cout reel. C'est la partie qu'on regrette de ne pas
 * avoir ecrite le jour ou la facture arrive.
 *
 * <p>Reglages : 10 questions, 1000 jetons, 500 caracteres par question.
 */
class QuotaPolicyTest {

    private final QuotaPolicy policy = new QuotaPolicy(new AssistantProperties("canned",
            new AssistantProperties.Quota(10, 1000, 500),
            new AssistantProperties.Context(10, 8000)));

    private final LocalDate today = LocalDate.of(2026, 8, 16);

    @Test
    @DisplayName("une question normale passe")
    void normalQuestionIsAllowed() {
        assertThat(policy.evaluate(fresh(), 100, 50))
                .isInstanceOf(QuotaPolicy.Verdict.Allowed.class);
    }

    @Test
    @DisplayName("une question trop longue est refusee avant tout appel")
    void oversizedQuestionIsDenied() {
        // Gratuit a verifier : autant le faire en premier.
        QuotaPolicy.Verdict verdict = policy.evaluate(fresh(), 5000, 10);

        assertThat(verdict).isInstanceOf(QuotaPolicy.Verdict.Denied.class);
        assertThat(((QuotaPolicy.Verdict.Denied) verdict).code())
                .isEqualTo("question_too_long");
    }

    @Test
    @DisplayName("le quota de questions epuise refuse")
    void exhaustedMessageQuotaDenies() {
        QuotaPolicy.Verdict verdict = policy.evaluate(usage(10, 0), 100, 50);

        assertThat(((QuotaPolicy.Verdict.Denied) verdict).code())
                .isEqualTo("daily_messages_exhausted");
    }

    @Test
    @DisplayName("le quota de jetons est evalue sur l'estimation, avant l'appel")
    void tokenQuotaIsCheckedOnTheEstimate() {
        // 900 deja consommes, 200 estimes : le total depasserait 1000. Attendre
        // le decompte reel reviendrait a payer la requete que l'on refuse.
        QuotaPolicy.Verdict verdict = policy.evaluate(usage(1, 900), 100, 200);

        assertThat(((QuotaPolicy.Verdict.Denied) verdict).code())
                .isEqualTo("daily_tokens_exhausted");
    }

    @Test
    @DisplayName("compter les questions ne suffit pas")
    void messageCountAloneIsNotEnough() {
        // Une seule question, mais qui a deja consomme presque tout le budget :
        // quelqu'un qui colle trois pages de cours coute cent fois plus qu'une
        // question courte.
        assertThat(policy.evaluate(usage(1, 990), 100, 100))
                .isInstanceOf(QuotaPolicy.Verdict.Denied.class);
    }

    @Test
    @DisplayName("la taille est verifiee avant le quota")
    void sizeIsCheckedBeforeQuota() {
        // Les deux sont en faute : c'est le motif le moins couteux a verifier qui
        // doit remonter, et le plus actionnable pour l'eleve.
        QuotaPolicy.Verdict verdict = policy.evaluate(usage(10, 999), 5000, 500);

        assertThat(((QuotaPolicy.Verdict.Denied) verdict).code())
                .isEqualTo("question_too_long");
    }

    @Test
    @DisplayName("le message de refus s'adresse a l'eleve, pas au journal")
    void denialReasonIsForTheStudent() {
        QuotaPolicy.Verdict verdict = policy.evaluate(usage(10, 0), 100, 50);
        String reason = ((QuotaPolicy.Verdict.Denied) verdict).reason();

        assertThat(reason).contains("demain");
        assertThat(reason).doesNotContain("quota").doesNotContain("token");
    }

    @Test
    @DisplayName("le reste se calcule sans jamais devenir negatif")
    void remainingNeverGoesNegative() {
        // Le leger depassement est accepte, le compteur peut donc depasser la
        // limite. Afficher "-3 questions restantes" n'aiderait personne.
        QuotaPolicy.Remaining remaining = policy.remainingFor(usage(12, 1200));

        assertThat(remaining.messages()).isZero();
        assertThat(remaining.tokens()).isZero();
    }

    @Test
    @DisplayName("le reste reflete la consommation")
    void remainingReflectsUsage() {
        QuotaPolicy.Remaining remaining = policy.remainingFor(usage(3, 250));

        assertThat(remaining.messages()).isEqualTo(7);
        assertThat(remaining.tokens()).isEqualTo(750);
        assertThat(remaining.maxQuestionChars()).isEqualTo(500);
    }

    private UsageQuota fresh() {
        return UsageQuota.empty("user-1", today);
    }

    private UsageQuota usage(int messages, int tokens) {
        return UsageQuota.empty("user-1", today).plus(messages, tokens);
    }
}
