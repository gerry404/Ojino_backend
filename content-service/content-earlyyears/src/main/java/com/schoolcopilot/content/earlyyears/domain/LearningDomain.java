package com.schoolcopilot.content.earlyyears.domain;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Un domaine d'apprentissage de la maternelle et du CP.
 *
 * <p>Ce n'est pas une matiere. On n'enseigne pas "les mathematiques" en petite
 * section : on construit les premiers outils pour structurer sa pensee. Plaquer
 * le vocabulaire du secondaire ici produirait un referentiel faux, et une
 * application qui parlerait a un enfant de quatre ans comme a un lyceen.
 *
 * <p>C'est la raison d'etre du module {@code content-earlyyears}.
 *
 * @param levelCodes classes concernees. Vide vaut "tout le cycle".
 */
@Document(collection = "early_years_domains")
@CompoundIndex(name = "idx_domain_system_code", def = "{'systemCode': 1, 'code': 1}", unique = true)
public record LearningDomain(
        @Id String id,
        @Indexed String systemCode,
        String code,
        String label,
        String description,
        List<String> levelCodes,
        int displayOrder,
        boolean archived) {

    /** Une liste vide vaut "aucune restriction de niveau". */
    public boolean appliesTo(String levelCode) {
        return levelCodes == null || levelCodes.isEmpty()
                || (levelCode != null && levelCodes.contains(levelCode));
    }
}
