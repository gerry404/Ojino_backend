package com.schoolcopilot.content.core.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Un exercice rattache a une notion.
 *
 * <p><strong>Le corrige ne sort jamais avec l'enonce.</strong> Il a sa propre
 * route, et plus tard sa propre condition — avoir tente. Les exposer ensemble
 * suffirait a vider l'exercice de son interet, et aucune interface cliente ne
 * pourrait rattraper cela.
 *
 * @param estimatedMinutes duree indicative, utilisee plus tard par le
 *        planificateur pour remplir un creneau de revision
 */
@Document(collection = "exercises")
@CompoundIndex(name = "idx_exercise_system_code", def = "{'systemCode': 1, 'code': 1}",
        unique = true)
@CompoundIndex(name = "idx_exercise_notion",
        def = "{'systemCode': 1, 'notionCode': 1, 'difficulty': 1}")
public record Exercise(
        @Id String id,
        @Indexed String systemCode,
        @Indexed String notionCode,
        String code,
        String statement,
        String solution,
        Difficulty difficulty,
        int estimatedMinutes,
        PublicationStatus status,
        boolean archived) {

    public boolean isVisible() {
        return status == PublicationStatus.PUBLISHED && !archived;
    }
}
