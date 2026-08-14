package com.schoolcopilot.content.core.domain;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Une filiere : C, D, A4, TI, Science, Commercial...
 *
 * @param levelCodes niveaux ou cette filiere existe. Une Terminale D n'a pas de
 *        sens en 5e, et le parcours d'inscription ne la proposera donc pas.
 * @param archived retiree des choix proposes, mais toujours resolvable
 */
@Document(collection = "tracks")
@CompoundIndex(name = "idx_track_system_code", def = "{'systemCode': 1, 'code': 1}", unique = true)
public record Track(
        @Id String id,
        @Indexed String systemCode,
        String code,
        String label,
        String description,
        List<String> levelCodes,
        int displayOrder,
        boolean archived) {

    public boolean availableAt(String levelCode) {
        return levelCodes != null && levelCodes.contains(levelCode);
    }
}
