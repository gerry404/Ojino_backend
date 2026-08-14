package com.schoolcopilot.user_service.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Etape 3 : systeme scolaire et classe. */
public record LevelRequest(

        @NotBlank(message = "Le systeme scolaire est obligatoire.")
        String systemCode,

        @NotBlank(message = "Le niveau est obligatoire.")
        String levelCode) {
}
