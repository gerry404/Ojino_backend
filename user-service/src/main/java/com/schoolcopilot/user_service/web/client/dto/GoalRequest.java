package com.schoolcopilot.user_service.web.client.dto;

import com.schoolcopilot.user_service.domain.profile.Goal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Etape 6 : l'objectif.
 *
 * @param targetExam examen ou concours vise, quand l'objectif en suppose un
 * @param note precision libre de l'eleve
 */
public record GoalRequest(

        @NotNull(message = "L'objectif est obligatoire.")
        Goal goal,

        @Size(max = 80, message = "Le nom de l'examen est trop long.")
        String targetExam,

        @Size(max = 500, message = "La precision est trop longue.")
        String note) {
}
