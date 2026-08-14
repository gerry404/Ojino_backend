package com.schoolcopilot.content.core.web.admin.dto;

import java.util.List;

import com.schoolcopilot.content.core.domain.Track;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

/**
 * Creation ou modification d'une filiere.
 *
 * @param levelCodes classes ou cette filiere existe. Obligatoire : une filiere
 *        rattachee a aucun niveau ne serait proposee nulle part.
 */
public record TrackUpsertRequest(

        @NotBlank(message = "Le code est obligatoire.")
        String code,

        @NotBlank(message = "Le libelle est obligatoire.")
        String label,

        String description,

        @NotEmpty(message = "Indiquez au moins un niveau ou cette filiere existe.")
        List<String> levelCodes,

        int displayOrder) {

    public Track toDomain() {
        return new Track(null, null, code, label, description, levelCodes, displayOrder, false);
    }
}
