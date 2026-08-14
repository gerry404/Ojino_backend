package com.schoolcopilot.user_service.service.profile;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schoolcopilot.user_service.domain.profile.OnboardingStep;
import com.schoolcopilot.user_service.domain.profile.StudentProfile;
import com.schoolcopilot.user_service.service.reference.ReferenceService;

/**
 * L'etat du parcours de creation de profil.
 *
 * <p>C'est le serveur qui decide de la sequence, et les applications se contentent
 * de l'afficher. Deux consequences utiles : le mobile et le web ne peuvent pas
 * diverger, et modifier le parcours ne demande pas de publier une nouvelle version
 * sur les stores.
 */
@Service
public class OnboardingService {

    private final ReferenceService reference;

    public OnboardingService(ReferenceService reference) {
        this.reference = reference;
    }

    /**
     * @param applicable une etape peut ne pas concerner cet eleve : la filiere ne
     *        se pose pas en 5e
     */
    public record StepState(OnboardingStep step, boolean applicable, boolean required,
            boolean completed) {
    }

    public record State(
            List<StepState> steps,
            OnboardingStep nextStep,
            int completedCount,
            int applicableCount,
            boolean complete) {
    }

    public State stateOf(StudentProfile profile) {
        List<StepState> steps = OnboardingStep.ordered().stream()
                .map(step -> new StepState(step, isApplicable(step, profile), step.required(),
                        profile.hasCompleted(step)))
                .toList();

        List<StepState> applicable = steps.stream().filter(StepState::applicable).toList();

        OnboardingStep next = applicable.stream()
                .filter(state -> !state.completed())
                .map(StepState::step)
                .findFirst()
                .orElse(null);

        boolean complete = applicable.stream()
                .filter(StepState::required)
                .allMatch(StepState::completed);

        return new State(steps, next,
                (int) applicable.stream().filter(StepState::completed).count(),
                applicable.size(),
                complete);
    }

    /** Vrai si toutes les etapes obligatoires qui concernent cet eleve sont faites. */
    public boolean isComplete(StudentProfile profile) {
        return stateOf(profile).complete();
    }

    /**
     * Seule la filiere est conditionnelle : elle ne se pose que pour les niveaux
     * qui s'y pretent. Tant que le niveau n'est pas choisi, l'etape reste affichee
     * pour que l'eleve voie le parcours complet devant lui.
     */
    private boolean isApplicable(OnboardingStep step, StudentProfile profile) {
        if (step != OnboardingStep.TRACK) {
            return true;
        }
        if (profile.getLevelCode() == null || profile.getSystemCode() == null) {
            return true;
        }
        return reference.requireLevel(profile.getSystemCode(), profile.getLevelCode()).hasTracks();
    }
}
