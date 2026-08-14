package com.schoolcopilot.user_service.domain.reference;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Une classe dans un systeme donne : 6e, Terminale, Form 5...
 *
 * <p>{@code typicalAgeMin} et {@code typicalAgeMax} servent a <em>suggerer</em> un
 * niveau a partir de l'age, jamais a en interdire un : un redoublement, une annee
 * d'avance ou une reprise d'etudes doivent rester possibles.
 *
 * @param hasTracks vrai quand ce niveau se decline en filieres, ce qui declenche
 *        l'etape correspondante dans le parcours d'inscription
 */
@Document(collection = "education_levels")
@CompoundIndex(name = "idx_level_system_code", def = "{'systemCode': 1, 'code': 1}", unique = true)
public record EducationLevel(
        @Id String id,
        @Indexed String systemCode,
        String code,
        String label,
        String cycle,
        int rank,
        int typicalAgeMin,
        int typicalAgeMax,
        boolean hasTracks) {

    /** Vrai si cet age tombe dans la tranche habituelle de la classe. */
    public boolean matchesAge(int age) {
        return age >= typicalAgeMin && age <= typicalAgeMax;
    }
}
