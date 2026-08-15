package com.schoolcopilot.engagement_service.domain;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Les compteurs de motivation d'un eleve et les badges deja obtenus.
 *
 * <p>Separe de la serie : celle-ci a sa propre logique et change tous les jours,
 * alors que ces compteurs ne font que croitre. Les melanger obligerait a reecrire
 * un document plus gros a chaque activite.
 */
@Document(collection = "engagement_profiles")
public record EngagementProfile(
        @Id String userId,
        Map<Badge.Metric, Integer> counters,
        Set<Badge> badges,
        Instant updatedAt) {

    public static EngagementProfile empty(String userId) {
        return new EngagementProfile(userId, new EnumMap<>(Badge.Metric.class),
                EnumSet.noneOf(Badge.class), Instant.now());
    }

    public Map<Badge.Metric, Integer> counters() {
        return counters == null ? Map.of() : counters;
    }

    public Set<Badge> badges() {
        return badges == null ? Set.of() : badges;
    }

    public int counter(Badge.Metric metric) {
        return counters().getOrDefault(metric, 0);
    }

    public boolean hasBadge(Badge badge) {
        return badges().contains(badge);
    }
}
