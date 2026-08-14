package com.schoolcopilot.user_service.web.client.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Superieur : le semestre en cours.
 *
 * <p>La borne haute n'est qu'un garde-fou de saisie ; c'est la duree reelle du
 * parcours qui fait foi, et elle est verifiee cote service.
 */
public record SemesterRequest(

        @Min(value = 1, message = "Le semestre commence a 1.")
        @Max(value = 20, message = "Semestre invraisemblable.")
        int semester) {
}
