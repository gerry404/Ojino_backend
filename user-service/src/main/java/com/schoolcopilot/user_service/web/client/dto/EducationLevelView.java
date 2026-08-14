package com.schoolcopilot.user_service.web.client.dto;

import com.schoolcopilot.user_service.service.reference.ReferenceService;

/**
 * Une classe proposee a l'inscription.
 *
 * <p>{@code suggested} met en avant celle qui correspond a l'age saisi, sans
 * jamais empecher d'en choisir une autre : redoublement, annee d'avance et
 * reprise d'etudes doivent rester possibles.
 *
 * @param hasTracks indique a l'application qu'une etape "filiere" suivra
 */
public record EducationLevelView(
        String code,
        String label,
        String cycle,
        int rank,
        int typicalAgeMin,
        int typicalAgeMax,
        boolean hasTracks,
        boolean suggested) {

    public static EducationLevelView from(ReferenceService.SuggestedLevel suggested) {
        return new EducationLevelView(
                suggested.level().code(),
                suggested.level().label(),
                suggested.level().cycle(),
                suggested.level().rank(),
                suggested.level().typicalAgeMin(),
                suggested.level().typicalAgeMax(),
                suggested.level().hasTracks(),
                suggested.suggested());
    }
}
