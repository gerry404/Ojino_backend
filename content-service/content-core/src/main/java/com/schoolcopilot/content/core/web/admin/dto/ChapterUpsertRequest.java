package com.schoolcopilot.content.core.web.admin.dto;

import com.schoolcopilot.content.core.domain.Chapter;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Creation ou modification d'un chapitre.
 *
 * <p>Ni statut ni archivage : chacun a sa propre route. Une modification de
 * libelle ne doit pas pouvoir publier un chapitre au passage.
 *
 * @param anchorCode matiere, domaine d'apprentissage ou unite d'enseignement,
 *        selon le cycle du niveau. Le module du cycle verifie qu'il existe.
 * @param trackCode facultatif. Null vaut "toutes les filieres de ce niveau", ce
 *        qui est le cas de la plupart des chapitres du tronc commun.
 */
public record ChapterUpsertRequest(

        @NotBlank(message = "Le code est obligatoire.")
        @Size(max = 40, message = "Le code est trop long.")
        String code,

        @NotBlank(message = "Le libelle est obligatoire.")
        String label,

        @Size(max = 1000, message = "Le resume est trop long.")
        String summary,

        @NotBlank(message = "L'ancrage est obligatoire.")
        String anchorCode,

        String trackCode,

        @Min(value = 1, message = "Le rang commence a 1.")
        int rank) {

    public Chapter toDomain() {
        return new Chapter(null, null, null, null, anchorCode, trackCode, code, label, summary,
                rank, null, false);
    }
}
