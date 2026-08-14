package com.schoolcopilot.content.earlyyears;

import java.util.List;

import org.springframework.stereotype.Component;

import com.schoolcopilot.content.core.domain.EducationCycle;
import com.schoolcopilot.content.core.spi.AnchorKind;
import com.schoolcopilot.content.core.spi.CurriculumModule;
import com.schoolcopilot.content.core.spi.CurriculumStep;
import com.schoolcopilot.content.earlyyears.repository.LearningDomainRepository;

/**
 * Maternelle jusqu'au CP.
 *
 * <p>Apres avoir choisi sa classe, l'enfant — ou son parent — ne choisit ni
 * filiere ni matiere : le cycle ne raisonne qu'en domaines d'apprentissage.
 *
 * <p><strong>Ce module est destine a devenir une application separee.</strong>
 * Il ne dependra donc jamais que de {@code content-core}, et aucun autre module
 * ne doit dependre de lui. Le jour de l'extraction, il part tel quel.
 */
@Component
public class EarlyYearsCurriculumModule implements CurriculumModule {

    private final LearningDomainRepository domains;

    public EarlyYearsCurriculumModule(LearningDomainRepository domains) {
        this.domains = domains;
    }

    /** Ici un chapitre se rattache a un domaine, jamais a une matiere. */
    @Override
    public AnchorKind anchorKind() {
        return AnchorKind.LEARNING_DOMAIN;
    }

    @Override
    public boolean anchorExists(String systemCode, String anchorCode) {
        return domains.findBySystemCodeAndCode(systemCode, anchorCode)
                .filter(domain -> !domain.archived())
                .isPresent();
    }

    @Override
    public EducationCycle cycle() {
        return EducationCycle.EARLY_YEARS;
    }

    @Override
    public String label() {
        return "Maternelle et CP";
    }

    @Override
    public List<CurriculumStep> steps() {
        return List.of(CurriculumStep.LEARNING_DOMAINS);
    }
}
