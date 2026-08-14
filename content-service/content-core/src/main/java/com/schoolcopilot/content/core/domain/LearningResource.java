package com.schoolcopilot.content.core.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Un support d'apprentissage rattache a une notion : lecon, fiche, video, lien.
 *
 * <p>Nomme {@code LearningResource} et non {@code Resource} pour ne pas entrer en
 * collision avec {@code org.springframework.core.io.Resource}, qu'on croise
 * partout ailleurs dans le projet.
 *
 * @param body contenu redige, en Markdown, pour une lecon ou une fiche
 * @param url adresse externe pour une video ou un lien
 */
@Document(collection = "learning_resources")
@CompoundIndex(name = "idx_resource_system_code", def = "{'systemCode': 1, 'code': 1}",
        unique = true)
@CompoundIndex(name = "idx_resource_notion", def = "{'systemCode': 1, 'notionCode': 1, 'rank': 1}")
public record LearningResource(
        @Id String id,
        @Indexed String systemCode,
        @Indexed String notionCode,
        String code,
        ResourceType type,
        String label,
        String body,
        String url,
        int rank,
        PublicationStatus status,
        boolean archived) {

    public boolean isVisible() {
        return status == PublicationStatus.PUBLISHED && !archived;
    }
}
