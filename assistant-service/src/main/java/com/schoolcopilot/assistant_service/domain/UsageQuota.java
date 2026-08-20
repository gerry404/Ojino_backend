package com.schoolcopilot.assistant_service.domain;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * La consommation d'un eleve sur une journee.
 *
 * <p>Un document par eleve et par jour, avec un identifiant deterministe : la
 * remise a zero se fait donc toute seule, sans tache planifiee ni risque de la
 * voir echouer un matin.
 *
 * <p>L'index TTL efface les journees anciennes : la consommation d'il y a six
 * mois n'interesse personne, et l'accumuler indefiniment n'a aucun benefice.
 */
@Document(collection = "usage_quotas")
public record UsageQuota(
        @Id String id,
        String userId,
        LocalDate day,
        int messages,
        int tokens,
        @Indexed(expireAfter = "90d") Instant createdAt) {

    public static String idFor(String userId, LocalDate day) {
        return userId + ":" + day;
    }

    public static UsageQuota empty(String userId, LocalDate day) {
        return new UsageQuota(idFor(userId, day), userId, day, 0, 0, Instant.now());
    }

    public UsageQuota plus(int extraMessages, int extraTokens) {
        return new UsageQuota(id, userId, day, messages + extraMessages, tokens + extraTokens,
                createdAt);
    }

    public int remainingMessages(int limit) {
        return Math.max(0, limit - messages);
    }

    public int remainingTokens(int limit) {
        return Math.max(0, limit - tokens);
    }
}
