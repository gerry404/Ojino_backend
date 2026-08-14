package com.schoolcopilot.user_service.web.client.dto;

import jakarta.validation.constraints.NotBlank;

/** Etape 4 : la filiere. Etape sautee pour les niveaux qui n'en ont pas. */
public record TrackRequest(

        @NotBlank(message = "La filiere est obligatoire.")
        String trackCode) {
}
