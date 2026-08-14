package com.schoolcopilot.content.highschool;

import java.util.List;

import org.springframework.stereotype.Component;

import com.schoolcopilot.content.core.domain.EducationCycle;
import com.schoolcopilot.content.core.spi.CurriculumModule;
import com.schoolcopilot.content.core.spi.CurriculumStep;

/**
 * Lycée et high school.
 *
 * <p>Le seul cycle du secondaire ou la filiere precede les matieres : on choisit
 * d'abord une Terminale D, et les matieres en decoulent.
 *
 * <p>Comme le college, ce module reste mince : filiere et matiere sont du
 * vocabulaire partage, porte par {@code content-core}. Il accueillera ce qui lui
 * est propre : le Probatoire et le Baccalaureat, les coefficients par filiere,
 * les epreuves.
 */
@Component
public class HighSchoolCurriculumModule implements CurriculumModule {

    @Override
    public EducationCycle cycle() {
        return EducationCycle.HIGH_SCHOOL;
    }

    @Override
    public String label() {
        return "Lycée";
    }

    @Override
    public List<CurriculumStep> steps() {
        return List.of(CurriculumStep.TRACK, CurriculumStep.SUBJECTS);
    }
}
