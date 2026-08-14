package com.schoolcopilot.learning_service.web.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.schoolcopilot.learning_service.domain.MasteryLevel;
import com.schoolcopilot.learning_service.domain.NotionMastery;

/**
 * L'etat de maitrise d'une notion.
 *
 * <p>{@code level} est ce que les applications affichent ; {@code score} reste
 * expose pour les usages qui en ont besoin, comme le classement des priorites de
 * revision.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MasteryView(
        String notionCode,
        MasteryLevel level,
        double score,
        int attempts,
        Instant lastEventAt) {

    public static MasteryView from(NotionMastery mastery) {
        return new MasteryView(mastery.notionCode(), mastery.level(), mastery.score(),
                mastery.attempts(), mastery.lastEventAt());
    }
}
