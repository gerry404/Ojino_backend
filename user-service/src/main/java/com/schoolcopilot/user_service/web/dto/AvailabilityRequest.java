package com.schoolcopilot.user_service.web.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.schoolcopilot.user_service.domain.profile.AvailabilitySlot;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Etape 8 : quand l'eleve peut travailler.
 *
 * <p>Les heures sont locales, sans fuseau : "18:00" veut dire 18h chez lui, ce qui
 * est le sens attendu pour un emploi du temps.
 */
public record AvailabilityRequest(

        @NotEmpty(message = "Indiquez au moins un creneau.")
        @Valid
        List<Slot> slots) {

    public record Slot(

            @NotNull(message = "Le jour est obligatoire.")
            DayOfWeek day,

            @NotNull(message = "L'heure de debut est obligatoire.")
            @JsonFormat(pattern = "HH:mm")
            LocalTime startTime,

            @NotNull(message = "L'heure de fin est obligatoire.")
            @JsonFormat(pattern = "HH:mm")
            LocalTime endTime) {

        public AvailabilitySlot toDomain() {
            return new AvailabilitySlot(day, startTime, endTime);
        }
    }

    public List<AvailabilitySlot> toDomain() {
        return slots.stream().map(Slot::toDomain).toList();
    }
}
