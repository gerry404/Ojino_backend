package com.schoolcopilot.content.core.web.admin.dto;

import com.schoolcopilot.content.core.domain.LearningResource;
import com.schoolcopilot.content.core.domain.ResourceType;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param body obligatoire pour une lecon ou une fiche
 * @param url obligatoire pour une video ou un lien
 */
public record ResourceUpsertRequest(

        @NotBlank(message = "Le code est obligatoire.")
        @Size(max = 40, message = "Le code est trop long.")
        String code,

        @NotNull(message = "Le type est obligatoire.")
        ResourceType type,

        @NotBlank(message = "Le libelle est obligatoire.")
        String label,

        String body,

        String url,

        @Min(value = 1, message = "Le rang commence a 1.")
        int rank) {

    public LearningResource toDomain() {
        return new LearningResource(null, null, null, code, type, label, body, url, rank, null,
                false);
    }
}
