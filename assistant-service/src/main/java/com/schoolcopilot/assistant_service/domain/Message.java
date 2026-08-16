package com.schoolcopilot.assistant_service.domain;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Un message du fil.
 *
 * <p>Les champs techniques — jetons, modele, moteur — sont conserves pour la
 * tracabilite : comprendre une mauvaise reponse six mois plus tard suppose de
 * savoir qui l'a produite et a quel cout. Ils ne sortent jamais dans l'API.
 *
 * @param feedback pouce haut ou bas laisse par l'eleve. Base de l'evaluation
 *        qualite, et presque gratuit a poser maintenant.
 */
@Document(collection = "messages")
@CompoundIndex(name = "idx_message_conversation",
        def = "{'conversationId': 1, 'createdAt': 1}")
public record Message(
        @Id String id,
        @Indexed String conversationId,
        String userId,
        MessageRole role,
        String content,
        int inputTokens,
        int outputTokens,
        String model,
        List<String> citedNotions,
        Feedback feedback,
        String feedbackReason,
        Instant createdAt) {

    public List<String> citedNotions() {
        return citedNotions == null ? List.of() : citedNotions;
    }

    public int totalTokens() {
        return inputTokens + outputTokens;
    }

    /** Qui parle. */
    public enum MessageRole {
        USER,
        ASSISTANT
    }

    /** Ce que l'eleve pense de la reponse. */
    public enum Feedback {
        HELPFUL,
        NOT_HELPFUL
    }
}
