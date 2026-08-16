package com.schoolcopilot.assistant_service.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Un fil de discussion avec l'assistant.
 *
 * <p>Les messages vivent dans une collection separee. Les imbriquer ici ferait
 * reecrire un document de plus en plus gros a chaque echange, et Mongo plafonne
 * un document a seize mega-octets : une conversation active finirait par ne plus
 * pouvoir s'ecrire.
 *
 * @param notionCode notion sur laquelle porte le fil, si l'eleve est parti d'une
 *        notion precise. C'est ce qui permet d'ancrer les reponses.
 */
@Document(collection = "conversations")
@CompoundIndex(name = "idx_conversation_user", def = "{'userId': 1, 'updatedAt': -1}")
public record Conversation(
        @Id String id,
        @Indexed String userId,
        String title,
        String systemCode,
        String notionCode,
        int messageCount,
        Instant createdAt,
        Instant updatedAt,
        Instant archivedAt) {

    public boolean belongsTo(String candidate) {
        return userId.equals(candidate);
    }

    public boolean isArchived() {
        return archivedAt != null;
    }
}
