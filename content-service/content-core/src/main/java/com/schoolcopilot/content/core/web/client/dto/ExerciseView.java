package com.schoolcopilot.content.core.web.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.schoolcopilot.content.core.domain.Difficulty;
import com.schoolcopilot.content.core.domain.Exercise;

/**
 * Un exercice tel que les applications le voient.
 *
 * <p><strong>Le corrige n'y figure pas, et c'est structurel.</strong> Il n'existe
 * aucun champ pour le porter, donc aucun oubli ne peut le faire fuiter. Il
 * s'obtient par une route dediee.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExerciseView(
        String code,
        String statement,
        Difficulty difficulty,
        int estimatedMinutes) {

    public static ExerciseView from(Exercise exercise) {
        return new ExerciseView(exercise.code(), exercise.statement(), exercise.difficulty(),
                exercise.estimatedMinutes());
    }
}
