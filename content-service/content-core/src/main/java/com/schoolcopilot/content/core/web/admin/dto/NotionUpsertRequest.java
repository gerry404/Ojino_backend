package com.schoolcopilot.content.core.web.admin.dto;

import java.util.List;

import com.schoolcopilot.content.core.domain.Notion;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Creation ou modification d'une notion.
 *
 * <p>Les prerequis ne sont pas ici : ils ont leur propre route, parce que les
 * declarer demande de verifier le graphe entier. Melanger les deux ferait echouer
 * une simple correction de libelle sur un probleme de cycle.
 */
public record NotionUpsertRequest(

        @NotBlank(message = "Le code est obligatoire.")
        @Size(max = 40, message = "Le code est trop long.")
        String code,

        @NotBlank(message = "Le libelle est obligatoire.")
        String label,

        @Size(max = 1000, message = "Le resume est trop long.")
        String summary,

        @Min(value = 1, message = "Le rang commence a 1.")
        int rank) {

    public Notion toDomain() {
        return new Notion(null, null, null, code, label, summary, rank, List.of(), null, false);
    }

    /** Remplace la liste complete des prerequis. Une liste vide les retire tous. */
    public record Prerequisites(
            @NotNull(message = "La liste est obligatoire, meme vide.")
            List<String> codes) {
    }
}
