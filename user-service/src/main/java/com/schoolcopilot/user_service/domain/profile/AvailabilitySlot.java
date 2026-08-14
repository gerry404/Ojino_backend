package com.schoolcopilot.user_service.domain.profile;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;

/**
 * Un creneau de travail, par exemple mardi de 18h a 19h30.
 *
 * <p>Les heures sont locales et sans fuseau : "18h" veut dire 18h chez l'eleve,
 * ce qui est exactement le sens voulu pour un emploi du temps.
 */
public record AvailabilitySlot(DayOfWeek day, LocalTime startTime, LocalTime endTime) {

    public Duration duration() {
        return Duration.between(startTime, endTime);
    }

    public long minutes() {
        return duration().toMinutes();
    }

    public boolean isValid() {
        return day != null && startTime != null && endTime != null && endTime.isAfter(startTime);
    }

    /** Vrai si les deux creneaux tombent le meme jour et se chevauchent. */
    public boolean overlaps(AvailabilitySlot other) {
        return day == other.day
                && startTime.isBefore(other.endTime)
                && other.startTime.isBefore(endTime);
    }
}
