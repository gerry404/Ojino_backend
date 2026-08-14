package com.schoolcopilot.content.core.web.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.schoolcopilot.content.core.domain.Chapter;

/**
 * Un chapitre tel que les applications le voient.
 *
 * <p>Ni le statut editorial ni l'archivage n'y figurent : ce qui arrive ici est
 * publie par construction, et l'etat interne du back-office ne regarde pas
 * l'eleve.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChapterView(
        String code,
        String label,
        String summary,
        String anchorCode,
        String trackCode,
        int rank) {

    public static ChapterView from(Chapter chapter) {
        return new ChapterView(chapter.code(), chapter.label(), chapter.summary(),
                chapter.anchorCode(), chapter.trackCode(), chapter.rank());
    }
}
