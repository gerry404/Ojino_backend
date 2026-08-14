package com.schoolcopilot.planning_service.web.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.schoolcopilot.planning_service.domain.Deadline;
import com.schoolcopilot.planning_service.domain.DeadlineType;

/**
 * Une echeance telle que les applications la voient.
 *
 * @param daysLeft calcule a l'affichage : c'est le compte a rebours, et il ne se
 *        stocke pas puisqu'il change chaque jour
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeadlineView(
        String id,
        DeadlineType type,
        String label,
        String anchorCode,
        List<String> notionCodes,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dueOn,
        long daysLeft,
        int importance,
        boolean completed) {

    public static DeadlineView from(Deadline deadline) {
        return new DeadlineView(deadline.id(), deadline.type(), deadline.label(),
                deadline.anchorCode(), deadline.notionCodes(), deadline.dueOn(),
                deadline.daysUntil(LocalDate.now()), deadline.importance(), deadline.completed());
    }
}
