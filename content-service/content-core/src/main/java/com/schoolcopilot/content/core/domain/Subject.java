package com.schoolcopilot.content.core.domain;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Une matiere.
 *
 * <p>Les listes {@code levelCodes} et {@code trackCodes} restreignent la matiere.
 * Vides, elles signifient "partout dans ce systeme" : la philosophie ne concerne
 * que le lycee, l'anglais tout le monde.
 *
 * @param core matiere du tronc commun, prochee par defaut a l'inscription
 * @param archived retiree des choix proposes, mais toujours resolvable
 */
@Document(collection = "subjects")
@CompoundIndex(name = "idx_subject_system_code", def = "{'systemCode': 1, 'code': 1}", unique = true)
public record Subject(
        @Id String id,
        @Indexed String systemCode,
        String code,
        String label,
        List<String> levelCodes,
        List<String> trackCodes,
        boolean core,
        int displayOrder,
        boolean archived) {

    /** Une liste vide vaut "aucune restriction". */
    public boolean appliesTo(String levelCode, String trackCode) {
        return matches(levelCodes, levelCode) && matches(trackCodes, trackCode);
    }

    private boolean matches(List<String> restriction, String value) {
        if (restriction == null || restriction.isEmpty()) {
            return true;
        }
        return value != null && restriction.contains(value);
    }
}
