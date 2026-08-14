package com.schoolcopilot.content_service.web.admin.dto;

import java.util.List;

import com.schoolcopilot.content_service.domain.reference.Subject;

import jakarta.validation.constraints.NotBlank;

/**
 * Creation ou modification d'une matiere.
 *
 * <p>{@code levelCodes} et {@code trackCodes} restreignent la matiere. Laisses
 * vides, ils valent "partout dans ce systeme" : l'anglais concerne tout le monde,
 * la philosophie seulement la Terminale.
 *
 * @param core matiere du tronc commun, prochee par defaut a l'inscription
 */
public record SubjectUpsertRequest(

        @NotBlank(message = "Le code est obligatoire.")
        String code,

        @NotBlank(message = "Le libelle est obligatoire.")
        String label,

        List<String> levelCodes,

        List<String> trackCodes,

        boolean core,

        int displayOrder) {

    public Subject toDomain() {
        return new Subject(null, null, code, label,
                levelCodes == null ? List.of() : levelCodes,
                trackCodes == null ? List.of() : trackCodes,
                core, displayOrder, false);
    }
}
