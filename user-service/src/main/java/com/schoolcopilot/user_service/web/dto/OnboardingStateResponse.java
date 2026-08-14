package com.schoolcopilot.user_service.web.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.schoolcopilot.user_service.domain.profile.OnboardingStep;
import com.schoolcopilot.user_service.service.profile.OnboardingService;

/**
 * L'etat du parcours d'inscription.
 *
 * <p>C'est la seule chose dont l'application a besoin pour afficher le wizard :
 * quelles etapes existent, lesquelles sont faites, et laquelle vient ensuite.
 *
 * @param nextStep null quand il ne reste plus rien a remplir
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OnboardingStateResponse(
        List<StepView> steps,
        OnboardingStep nextStep,
        int completedCount,
        int applicableCount,
        boolean complete,
        ProfileResponse profile) {

    /**
     * @param applicable faux pour une etape qui ne concerne pas cet eleve : la
     *        filiere ne se pose pas en 5e, l'application ne l'affiche donc pas
     */
    public record StepView(OnboardingStep step, boolean applicable, boolean required,
            boolean completed) {
    }

    public static OnboardingStateResponse of(OnboardingService.State state,
            ProfileResponse profile) {
        return new OnboardingStateResponse(
                state.steps().stream()
                        .map(step -> new StepView(step.step(), step.applicable(), step.required(),
                                step.completed()))
                        .toList(),
                state.nextStep(),
                state.completedCount(),
                state.applicableCount(),
                state.complete(),
                profile);
    }
}
