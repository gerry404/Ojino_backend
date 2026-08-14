package com.schoolcopilot.user_service.web.client.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

/** Etape 5 : les matieres suivies. */
public record SubjectsRequest(

        @NotEmpty(message = "Choisissez au moins une matiere.")
        List<String> subjectCodes) {
}
