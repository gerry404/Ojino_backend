package com.schoolcopilot.assistant_service.service;

import org.springframework.stereotype.Component;

import com.schoolcopilot.assistant_service.config.AssistantProperties;
import com.schoolcopilot.assistant_service.domain.UsageQuota;

/**
 * Decide si une question peut partir vers le moteur.
 *
 * <p>Logique pure, isolee a dessein : c'est elle qui arbitre un cout reel, et
 * c'est la partie qu'on regrette de ne pas avoir ecrite le jour ou la facture
 * arrive.
 *
 * <p>L'ordre des controles suit le meme principe que {@code DeliveryGate} dans
 * le service de notification : ce qui est gratuit a verifier passe en premier.
 */
@Component
public class QuotaPolicy {

    private final AssistantProperties properties;

    public QuotaPolicy(AssistantProperties properties) {
        this.properties = properties;
    }

    /** Verdict rendu avant l'appel au moteur. */
    public sealed interface Verdict {

        record Allowed() implements Verdict {
        }

        /** @param reason destine a l'eleve, pas au journal technique */
        record Denied(String code, String reason) implements Verdict {
        }
    }

    /**
     * @param questionLength longueur de la question
     * @param estimatedTokens estimation du cout, avant appel
     */
    public Verdict evaluate(UsageQuota usage, int questionLength, int estimatedTokens) {
        AssistantProperties.Quota limits = properties.quota();

        // 1. La taille de l'entree : gratuit a verifier, et refuse tout de suite
        //    ce qui n'a aucune chance de passer.
        if (questionLength > limits.maxInputChars()) {
            return new Verdict.Denied("question_too_long",
                    "Ta question est trop longue. Essaie de la resumer en "
                            + limits.maxInputChars() + " caracteres.");
        }

        // 2. Le nombre de questions du jour.
        if (usage.messages() >= limits.dailyMessages()) {
            return new Verdict.Denied("daily_messages_exhausted",
                    "Tu as atteint ta limite de questions pour aujourd'hui. "
                            + "Elle repart demain.");
        }

        // 3. Les jetons. Le controle porte sur l'estimation : les jetons reels ne
        //    sont connus qu'apres la reponse, et attendre pour verifier
        //    reviendrait a payer la requete que l'on refuse.
        if (usage.tokens() + estimatedTokens > limits.dailyTokens()) {
            return new Verdict.Denied("daily_tokens_exhausted",
                    "Tu as beaucoup echange avec l'assistant aujourd'hui. "
                            + "Reviens demain.");
        }

        return new Verdict.Allowed();
    }

    /** Ce qu'il reste a l'eleve, pour le lui montrer avant qu'il ne bute dessus. */
    public record Remaining(int messages, int tokens, int maxQuestionChars) {
    }

    public Remaining remainingFor(UsageQuota usage) {
        AssistantProperties.Quota limits = properties.quota();
        return new Remaining(
                usage.remainingMessages(limits.dailyMessages()),
                usage.remainingTokens(limits.dailyTokens()),
                limits.maxInputChars());
    }
}
