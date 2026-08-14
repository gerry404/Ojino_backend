package com.schoolcopilot.content_service.web.admin.dto;

import com.schoolcopilot.content_service.domain.reference.EducationLevel;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Creation ou modification d'une classe.
 *
 * <p>{@code rank} ordonne les classes du systeme, et la tranche d'age sert a
 * suggerer un niveau a l'inscription.
 *
 * @param hasTracks declenche ou non l'etape "filiere" du parcours d'inscription
 */
public record LevelUpsertRequest(

        @NotBlank(message = "Le code est obligatoire.")
        String code,

        @NotBlank(message = "Le libelle est obligatoire.")
        String label,

        @NotBlank(message = "Le cycle est obligatoire.")
        String cycle,

        @Min(value = 1, message = "Le rang commence a 1.")
        int rank,

        @Min(value = 3, message = "L'age minimum est trop bas.")
        @Max(value = 100, message = "L'age minimum est trop eleve.")
        int typicalAgeMin,

        @Min(value = 3, message = "L'age maximum est trop bas.")
        @Max(value = 100, message = "L'age maximum est trop eleve.")
        int typicalAgeMax,

        boolean hasTracks) {

    public EducationLevel toDomain() {
        return new EducationLevel(null, null, code, label, cycle, rank, typicalAgeMin,
                typicalAgeMax, hasTracks, false);
    }
}
