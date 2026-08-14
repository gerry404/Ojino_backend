package com.schoolcopilot.user_service.web.admin.dto;

import com.schoolcopilot.user_service.domain.reference.EducationSystem;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Creation ou modification d'un systeme scolaire.
 *
 * <p>{@code code} n'est lu qu'a la creation : renommer un systeme casserait tous
 * les profils qui s'y rattachent, cela se fait par migration et non par une
 * modification en place.
 */
public record SystemUpsertRequest(

        @NotBlank(message = "Le code est obligatoire.")
        @Size(max = 20, message = "Le code est trop long.")
        String code,

        @NotBlank(message = "Le code pays est obligatoire.")
        @Size(min = 2, max = 2, message = "Le code pays suit la norme ISO a deux lettres.")
        String country,

        @NotBlank(message = "Le nom du pays est obligatoire.")
        String countryLabel,

        @NotBlank(message = "Le libelle est obligatoire.")
        String label,

        @NotBlank(message = "La langue est obligatoire.")
        String language,

        int displayOrder,

        boolean active) {

    public EducationSystem toDomain() {
        return new EducationSystem(code, country, countryLabel, label, language, displayOrder,
                active);
    }
}
