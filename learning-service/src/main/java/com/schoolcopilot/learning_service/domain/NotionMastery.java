package com.schoolcopilot.learning_service.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * L'etat de maitrise d'un eleve sur une notion.
 *
 * <p>Donnee <strong>derivee</strong> : elle se recalcule integralement a partir
 * des {@link LearningEvent}. La perdre ne perdrait rien — c'est justement ce qui
 * autorise a changer d'algorithme de notation et a tout rejouer.
 *
 * <p>Elle existe quand meme, plutot que d'etre recalculee a la volee, parce
 * qu'elle est lue en permanence : par le planificateur pour choisir quoi
 * reviser, par l'assistant pour adapter ses explications.
 *
 * @param attempts nombre d'evenements notes pris en compte
 */
@Document(collection = "notion_mastery")
@CompoundIndex(name = "idx_mastery_user_notion",
        def = "{'userId': 1, 'systemCode': 1, 'notionCode': 1}", unique = true)
@CompoundIndex(name = "idx_mastery_user_level", def = "{'userId': 1, 'level': 1}")
public record NotionMastery(
        @Id String id,
        @Indexed String userId,
        String systemCode,
        String notionCode,
        double score,
        int attempts,
        MasteryLevel level,
        Instant lastEventAt,
        Instant updatedAt) {

    public boolean isMastered() {
        return level == MasteryLevel.MASTERED;
    }

    public boolean needsWork() {
        return level == MasteryLevel.STRUGGLING || level == MasteryLevel.UNKNOWN;
    }

    /** Identifiant deterministe : un eleve n'a qu'un etat par notion. */
    public static String idFor(String userId, String systemCode, String notionCode) {
        return userId + ":" + systemCode + ":" + notionCode;
    }
}
