package com.schoolcopilot.content_service.web.client.dto;

import com.schoolcopilot.content_service.domain.reference.EducationLevel;
import com.schoolcopilot.content_service.service.reference.ReferenceService;

/**
 * Une classe proposee a l'inscription.
 *
 * <p>{@code suggested} met en avant celle qui correspond a l'age saisi, sans
 * jamais empecher d'en choisir une autre : redoublement, annee d'avance et
 * reprise d'etudes doivent rester possibles.
 *
 * @param hasTracks indique a l'appelant qu'une etape "filiere" suivra
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
        return build(suggested.level(), suggested.suggested());
    }

    /** Hors contexte de suggestion : consultation d'une classe precise. */
    public static EducationLevelView of(EducationLevel level) {
        return build(level, false);
    }

    private static EducationLevelView build(EducationLevel level, boolean suggested) {
        return new EducationLevelView(
                level.code(),
                level.label(),
                level.cycle(),
                level.rank(),
                level.typicalAgeMin(),
                level.typicalAgeMax(),
                level.hasTracks(),
                suggested);
    }
}
