package com.schoolcopilot.planning_service.web.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.schoolcopilot.planning_service.domain.Deadline;
import com.schoolcopilot.planning_service.domain.DeadlineType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Une echeance a enregistrer.
 *
 * @param notionCodes ce que l'echeance couvre. Facultatif : sans precision, elle
 *        pese sur le planning sans cibler de notion particuliere.
 * @param importance de 1 a 5, pour arbitrer entre deux echeances le meme jour
 */
public record DeadlineRequest(

        @NotBlank(message = "Le systeme scolaire est obligatoire.")
        String systemCode,

        @NotNull(message = "Le type d'echeance est obligatoire.")
        DeadlineType type,

        @NotBlank(message = "Le libelle est obligatoire.")
        @Size(max = 200, message = "Le libelle est trop long.")
        String label,

        String anchorCode,

        List<String> notionCodes,

        @NotNull(message = "La date est obligatoire.")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate dueOn,

        @Min(value = 1, message = "L'importance va de 1 a 5.")
        @Max(value = 5, message = "L'importance va de 1 a 5.")
        int importance) {

    public Deadline toDomain() {
        return new Deadline(null, null, systemCode, type, label, anchorCode,
                notionCodes == null ? List.of() : notionCodes, dueOn, importance, false, null);
    }
}
