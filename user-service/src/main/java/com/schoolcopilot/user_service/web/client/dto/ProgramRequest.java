package com.schoolcopilot.user_service.web.client.dto;

import jakarta.validation.constraints.NotBlank;

/** Superieur : le parcours suivi. */
public record ProgramRequest(

        @NotBlank(message = "Le parcours est obligatoire.")
        String programCode) {
}
