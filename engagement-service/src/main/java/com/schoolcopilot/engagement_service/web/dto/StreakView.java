package com.schoolcopilot.engagement_service.web.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.schoolcopilot.engagement_service.domain.Streak;

/**
 * La serie telle que l'eleve la voit.
 *
 * <p>Les jokers sont exposes, et c'est volontaire : une serie qui tient grace a un
 * joker doit le dire. Un compteur qui bouge sans explication laisse croire a un
 * bug, et une reserve visible rassure sur ce qui arrive en cas d'absence.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StreakView(
        int current,
        int longest,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate lastActiveOn,
        int freezesAvailable,
        boolean activeToday) {

    public static StreakView from(Streak streak, LocalDate today) {
        return new StreakView(streak.current(), streak.longest(), streak.lastActiveOn(),
                streak.freezesAvailable(), streak.isActiveOn(today));
    }
}
