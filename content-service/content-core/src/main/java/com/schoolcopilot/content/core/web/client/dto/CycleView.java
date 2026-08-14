package com.schoolcopilot.content.core.web.client.dto;

import java.util.List;

import com.schoolcopilot.content.core.domain.EducationCycle;
import com.schoolcopilot.content.core.spi.CurriculumModule;
import com.schoolcopilot.content.core.spi.CurriculumStep;

/**
 * Un cycle propose par un systeme scolaire, et ce qu'il demande a l'eleve.
 *
 * <p>N'apparait que si le module Maven correspondant est embarque dans ce
 * deploiement : un serveur qui ne sert que le secondaire ne listera ni la
 * maternelle ni l'universite.
 */
public record CycleView(EducationCycle cycle, String label, List<CurriculumStep> steps) {

    public static CycleView from(CurriculumModule module) {
        return new CycleView(module.cycle(), module.label(), module.steps());
    }
}
