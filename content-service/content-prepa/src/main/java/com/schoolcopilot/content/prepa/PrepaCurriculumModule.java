package com.schoolcopilot.content.prepa;

import java.util.List;

import org.springframework.stereotype.Component;

import com.schoolcopilot.content.core.domain.EducationCycle;
import com.schoolcopilot.content.core.spi.CurriculumModule;
import com.schoolcopilot.content.core.spi.CurriculumStep;

/**
 * Classes preparatoires.
 *
 * <p>Meme sequence apparente que le lycee — filiere puis matieres — mais la
 * ressemblance s'arrete la. Une prepa se definit par les concours qu'elle vise,
 * un rythme de colles et des programmes bien plus denses. C'est ce qui justifie
 * un module distinct plutot qu'un cas particulier du lycee.
 *
 * <p>Aucun systeme livre ne contient encore de niveaux de prepa : le module est
 * en place, les donnees viendront.
 */
@Component
public class PrepaCurriculumModule implements CurriculumModule {

    @Override
    public EducationCycle cycle() {
        return EducationCycle.PREPA;
    }

    @Override
    public String label() {
        return "Classes préparatoires";
    }

    @Override
    public List<CurriculumStep> steps() {
        return List.of(CurriculumStep.TRACK, CurriculumStep.SUBJECTS);
    }
}
