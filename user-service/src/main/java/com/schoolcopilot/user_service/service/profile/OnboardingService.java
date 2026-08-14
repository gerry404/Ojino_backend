package com.schoolcopilot.user_service.service.profile;

import java.util.List;

import org.springframework.stereotype.Service;

import com.schoolcopilot.user_service.domain.profile.OnboardingStep;
import com.schoolcopilot.user_service.domain.profile.StudentProfile;

/**
 * L'etat du parcours de creation de profil.
 *
 * <p>C'est le serveur qui decide de la sequence, et les applications se contentent
 * de l'afficher. Deux consequences utiles : le mobile et le web ne peuvent pas
 * diverger, et modifier le parcours ne demande pas de publier une nouvelle version
 * sur les stores.
 *
 * <p>Ce service ne fait aucun appel reseau : tout ce dont il a besoin est sur le
 * profil. C'est important, car l'etat du parcours est lu a chaque ouverture de
 * l'application.
 */
@Service
public class OnboardingService {

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
     * Les etapes dependantes du cycle ne s'affichent que si le cycle les demande.
     *
     * <p>Un enfant de maternelle ne voit ni filiere ni matieres, mais des domaines
     * d'apprentissage ; un etudiant voit parcours, semestre et unites
     * d'enseignement. Ce service ne sait rien de tout cela : la sequence a ete
     * recopiee sur le profil au moment du choix de la classe, depuis
     * content-service.
     *
     * <p>Tant que la classe n'est pas choisie, ces etapes restent affichees pour
     * que l'eleve voie le parcours devant lui — elles se preciseront ensuite.
     */
    private boolean isApplicable(OnboardingStep step, StudentProfile profile) {
        if (!step.cycleDependent()) {
            return true;
        }
        if (profile.getLevelCode() == null) {
            return true;
        }
        return profile.requires(step);
    }
}
