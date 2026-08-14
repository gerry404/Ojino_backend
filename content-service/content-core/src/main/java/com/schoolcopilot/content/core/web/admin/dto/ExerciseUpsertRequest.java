package com.schoolcopilot.content.core.web.admin.dto;

import com.schoolcopilot.content.core.domain.Difficulty;
import com.schoolcopilot.content.core.domain.Exercise;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @param solution obligatoire : un exercice sans corrige ne permet ni de se
 *        verifier soi-meme, ni a l'assistant d'expliquer l'erreur
 * @param estimatedMinutes duree indicative, que le planificateur utilisera pour
 *        remplir un creneau de revision
 */
public record ExerciseUpsertRequest(

        @NotBlank(message = "Le code est obligatoire.")
        String code,

        @NotBlank(message = "L'enonce est obligatoire.")
        String statement,

        @NotBlank(message = "Le corrige est obligatoire.")
        String solution,

        @NotNull(message = "La difficulte est obligatoire.")
        Difficulty difficulty,

        @Min(value = 1, message = "La duree estimee doit valoir au moins une minute.")
        @Max(value = 240, message = "Au-dela de quatre heures, decoupez l'exercice.")
        int estimatedMinutes) {

    public Exercise toDomain() {
        return new Exercise(null, null, null, code, statement, solution, difficulty,
                estimatedMinutes, null, false);
    }
}
