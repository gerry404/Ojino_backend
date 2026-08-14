package com.schoolcopilot.content.core.web.client.dto;

import java.util.List;

import com.schoolcopilot.content.core.domain.EducationCycle;
import com.schoolcopilot.content.core.domain.EducationLevel;
import com.schoolcopilot.content.core.service.ReferenceService;
import com.schoolcopilot.content.core.spi.CurriculumStep;

/**
 * Une classe proposee a l'inscription.
 *
 * <p>{@code suggested} met en avant celle qui correspond a l'age saisi, sans
 * jamais empecher d'en choisir une autre : redoublement, annee d'avance et
 * reprise d'etudes doivent rester possibles.
 *
 * @param steps les choix que ce cycle demande apres celui de la classe. C'est ce
 *        qui permet au parcours d'inscription de s'adapter sans connaitre les
 *        cycles : le lycee renvoie {@code [TRACK, SUBJECTS]}, la maternelle
 *        {@code [LEARNING_DOMAINS]}, l'universite tout autre chose.
 */
public record EducationLevelView(
        String code,
        String label,
        EducationCycle cycle,
        List<CurriculumStep> steps,
        int rank,
        int typicalAgeMin,
        int typicalAgeMax,
        boolean hasTracks,
        boolean suggested) {

    public static EducationLevelView from(ReferenceService.SuggestedLevel suggested,
            List<CurriculumStep> steps) {
        return build(suggested.level(), steps, suggested.suggested());
    }

    /** Hors contexte de suggestion : consultation d'une classe precise. */
    public static EducationLevelView of(EducationLevel level, List<CurriculumStep> steps) {
        return build(level, steps, false);
    }

    private static EducationLevelView build(EducationLevel level, List<CurriculumStep> steps,
            boolean suggested) {
        return new EducationLevelView(
                level.code(),
                level.label(),
                level.cycle(),
                steps,
                level.rank(),
                level.typicalAgeMin(),
                level.typicalAgeMax(),
                level.hasTracks(),
                suggested);
    }
}
