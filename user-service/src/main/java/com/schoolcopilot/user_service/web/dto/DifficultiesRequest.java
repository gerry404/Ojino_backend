package com.schoolcopilot.user_service.web.dto;

import java.util.List;

import com.schoolcopilot.user_service.domain.profile.Difficulty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Etape 7 : les matieres en difficulte.
 *
 * <p>Une liste vide est une reponse valable et valide l'etape : tout le monde n'a
 * pas de matiere qui coince.
 */
public record DifficultiesRequest(

        @NotNull(message = "La liste est obligatoire, meme vide.")
        @Valid
        List<Item> items) {

    public record Item(

            @NotBlank(message = "La matiere est obligatoire.")
            String subjectCode,

            @Min(value = 1, message = "L'intensite va de 1 a 3.")
            @Max(value = 3, message = "L'intensite va de 1 a 3.")
            int severity,

            @Size(max = 300, message = "La precision est trop longue.")
            String note) {

        public Difficulty toDomain() {
            return new Difficulty(subjectCode, severity, note);
        }
    }

    public List<Difficulty> toDomain() {
        return items.stream().map(Item::toDomain).toList();
    }
}
