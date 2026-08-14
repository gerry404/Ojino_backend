package com.schoolcopilot.content.core.spi;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.schoolcopilot.content.core.domain.EducationCycle;

/**
 * L'annuaire des cycles presents dans l'application.
 *
 * <p>Il se remplit tout seul : Spring injecte toutes les implementations de
 * {@link CurriculumModule} qu'il trouve sur le classpath. Retirer le module Maven
 * d'un cycle le fait disparaitre d'ici sans qu'aucune ligne ne change.
 */
@Component
public class CurriculumModules {

    private final Map<EducationCycle, CurriculumModule> byCycle =
            new EnumMap<>(EducationCycle.class);

    public CurriculumModules(List<CurriculumModule> modules) {
        modules.forEach(module -> byCycle.put(module.cycle(), module));
    }

    /**
     * Vide si le cycle n'est pas embarque dans cette application — ce qui est un
     * etat normal, pas une erreur : un deploiement peut tres bien ne servir que le
     * secondaire.
     */
    public Optional<CurriculumModule> forCycle(EducationCycle cycle) {
        return Optional.ofNullable(byCycle.get(cycle));
    }

    public boolean supports(EducationCycle cycle) {
        return byCycle.containsKey(cycle);
    }

    /** Dans l'ordre naturel des cycles, de la maternelle au superieur. */
    public List<CurriculumModule> all() {
        return byCycle.values().stream()
                .sorted(Comparator.comparing(CurriculumModule::cycle))
                .toList();
    }
}
