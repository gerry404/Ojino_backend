package com.schoolcopilot.engagement_service.web.dto;

import java.time.LocalDate;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.schoolcopilot.engagement_service.domain.Badge;

import jakarta.validation.constraints.NotBlank;

/**
 * Une activite signalee par un autre service.
 *
 * @param day facultatif. Permet a une application hors ligne de remonter une
 *        activite passee a la bonne date, sans fausser la serie.
 * @param increments compteurs a incrementer : seances terminees, notions
 *        acquises. Le service qui signale sait ce qu'il vient de se passer,
 *        celui-ci n'a pas a le deviner.
 */
public record ActivityRequest(

        @NotBlank(message = "Le destinataire est obligatoire.")
        String userId,

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate day,

        Map<Badge.Metric, Integer> increments) {

    public Map<Badge.Metric, Integer> increments() {
        return increments == null ? Map.of() : increments;
    }
}
