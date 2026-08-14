package com.schoolcopilot.planning_service.web.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.schoolcopilot.planning_service.domain.SessionReason;
import com.schoolcopilot.planning_service.domain.SessionStatus;
import com.schoolcopilot.planning_service.domain.StudySession;

/**
 * Une seance telle que les applications la voient.
 *
 * <p>{@code reason} est expose volontairement : un planning qui explique ses
 * choix se suit, un planning opaque s'abandonne.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SessionView(
        String id,
        String notionCode,
        SessionReason reason,
        String deadlineId,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate scheduledOn,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime,
        int plannedMinutes,
        SessionStatus status,
        Integer actualMinutes) {

    public static SessionView from(StudySession session) {
        return new SessionView(session.id(), session.notionCode(), session.reason(),
                session.deadlineId(), session.scheduledOn(), session.startTime(),
                session.endTime(), session.plannedMinutes(), session.status(),
                session.actualMinutes());
    }
}
