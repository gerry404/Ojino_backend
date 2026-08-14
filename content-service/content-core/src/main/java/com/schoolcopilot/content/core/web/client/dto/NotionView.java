package com.schoolcopilot.content.core.web.client.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.schoolcopilot.content.core.domain.Notion;

/**
 * Une notion telle que les applications la voient.
 *
 * <p>{@code prerequisiteCodes} est expose : le client peut ainsi afficher "il te
 * manque ceci" sans un second appel.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotionView(
        String code,
        String label,
        String summary,
        String chapterCode,
        int rank,
        List<String> prerequisiteCodes) {

    public static NotionView from(Notion notion) {
        return new NotionView(notion.code(), notion.label(), notion.summary(),
                notion.chapterCode(), notion.rank(), notion.prerequisiteCodes());
    }
}
