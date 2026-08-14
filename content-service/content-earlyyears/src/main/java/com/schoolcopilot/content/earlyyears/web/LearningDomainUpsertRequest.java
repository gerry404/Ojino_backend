package com.schoolcopilot.content.earlyyears.web;

import java.util.List;

import com.schoolcopilot.content.earlyyears.domain.LearningDomain;

import jakarta.validation.constraints.NotBlank;

/**
 * @param levelCodes classes concernees. Vide vaut "tout le cycle", ce qui est le
 *        cas courant : les domaines de la maternelle traversent les trois
 *        sections.
 */
public record LearningDomainUpsertRequest(

        @NotBlank(message = "Le code est obligatoire.")
        String code,

        @NotBlank(message = "Le libelle est obligatoire.")
        String label,

        String description,

        List<String> levelCodes,

        int displayOrder) {

    public LearningDomain toDomain() {
        return new LearningDomain(null, null, code, label, description,
                levelCodes == null ? List.of() : levelCodes, displayOrder, false);
    }
}
