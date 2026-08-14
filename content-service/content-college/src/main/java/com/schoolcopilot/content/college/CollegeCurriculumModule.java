package com.schoolcopilot.content.college;

import java.util.List;

import org.springframework.stereotype.Component;

import com.schoolcopilot.content.core.domain.EducationCycle;
import com.schoolcopilot.content.core.repository.ReferenceRepositories;
import com.schoolcopilot.content.core.spi.SubjectAnchoredModule;
import com.schoolcopilot.content.core.spi.CurriculumStep;

/**
 * Collège et secondary.
 *
 * <p>Apres sa classe, l'eleve ne choisit que ses matieres. Certains systemes
 * introduisent une orientation des la Form 4 : c'est alors porte par
 * {@code hasTracks} sur le niveau, et ce module n'a rien a en dire.
 *
 * <p><strong>Module volontairement mince.</strong> Le college partage tout son
 * vocabulaire — niveau, matiere — avec le reste du secondaire, et ce vocabulaire
 * vit dans {@code content-core}. Le dupliquer ici pour "remplir" le module aurait
 * ete du bruit. Il existe pour accueillir ce qui lui est propre : le socle commun,
 * le BEPC et le brevet, les competences de fin de cycle.
 */
@Component
public class CollegeCurriculumModule extends SubjectAnchoredModule {

    public CollegeCurriculumModule(ReferenceRepositories.Subjects subjects) {
        super(subjects);
    }

    @Override
    public EducationCycle cycle() {
        return EducationCycle.COLLEGE;
    }

    @Override
    public String label() {
        return "Collège";
    }

    @Override
    public List<CurriculumStep> steps() {
        return List.of(CurriculumStep.SUBJECTS);
    }
}
