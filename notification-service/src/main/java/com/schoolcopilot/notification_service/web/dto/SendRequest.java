package com.schoolcopilot.notification_service.web.dto;

import java.util.Map;

import com.schoolcopilot.notification_service.domain.NotificationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Une demande d'envoi, emise par un autre service.
 *
 * @param values valeurs a injecter dans le gabarit du type
 * @param dedupeKey empeche le doublon. Fortement conseille pour tout ce qui vient
 *        d'un ordonnanceur : sans lui, un rappel rejoue produit deux vibrations.
 */
public record SendRequest(

        @NotBlank(message = "Le destinataire est obligatoire.")
        String userId,

        @NotNull(message = "Le type de notification est obligatoire.")
        NotificationType type,

        Map<String, String> values,

        String dedupeKey) {

    public Map<String, String> values() {
        return values == null ? Map.of() : values;
    }
}
