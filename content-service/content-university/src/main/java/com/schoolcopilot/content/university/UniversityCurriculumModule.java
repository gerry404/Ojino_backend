package com.schoolcopilot.content.university;

import java.util.List;

import org.springframework.stereotype.Component;

import com.schoolcopilot.content.core.domain.EducationCycle;
import com.schoolcopilot.content.core.spi.AnchorKind;
import com.schoolcopilot.content.core.spi.CurriculumModule;
import com.schoolcopilot.content.core.spi.CurriculumStep;
import com.schoolcopilot.content.university.repository.UniversityRepositories;

/**
 * Enseignement superieur.
 *
 * <p>Le cycle qui rompt le plus franchement avec le reste. Un etudiant n'a ni
 * classe, ni filiere, ni matieres : il a un parcours, des semestres, des unites
 * d'enseignement et des credits. C'est pourquoi ce module porte son propre
 * modele de donnees au lieu de reutiliser celui du secondaire.
 */
@Component
public class UniversityCurriculumModule implements CurriculumModule {

    private final UniversityRepositories.CourseUnits courseUnits;

    public UniversityCurriculumModule(UniversityRepositories.CourseUnits courseUnits) {
        this.courseUnits = courseUnits;
    }

    /** Ici un chapitre se rattache a une unite d'enseignement. */
    @Override
    public AnchorKind anchorKind() {
        return AnchorKind.COURSE_UNIT;
    }

    @Override
    public boolean anchorExists(String systemCode, String anchorCode) {
        return courseUnits.findBySystemCodeAndCode(systemCode, anchorCode)
                .filter(unit -> !unit.archived())
                .isPresent();
    }

    @Override
    public EducationCycle cycle() {
        return EducationCycle.UNIVERSITY;
    }

    @Override
    public String label() {
        return "Université";
    }

    @Override
    public List<CurriculumStep> steps() {
        return List.of(CurriculumStep.PROGRAM, CurriculumStep.SEMESTER,
                CurriculumStep.COURSE_UNITS);
    }
}
