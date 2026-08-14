package com.schoolcopilot.learning_service.web.dto;

import java.time.Instant;

import com.schoolcopilot.learning_service.domain.LearningEvent;
import com.schoolcopilot.learning_service.domain.LearningEventType;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Un fait d'apprentissage a enregistrer.
 *
 * <p>L'identifiant de l'eleve ne figure pas ici : il vient du token. Personne ne
 * peut donc enregistrer un resultat au nom d'un autre.
 *
 * @param weight importance relative. Laisser a 0 vaut 1 : un devoir surveille se
 *        declare a 3, un exercice d'entrainement a 1.
 * @param occurredAt facultatif. Permet a une application hors ligne de remonter
 *        ce qui s'est passe pendant la coupure, a la bonne date.
 */
public record RecordEventRequest(

        @NotBlank(message = "Le systeme scolaire est obligatoire.")
        String systemCode,

        @NotBlank(message = "La notion est obligatoire.")
        String notionCode,

        @NotNull(message = "Le type d'evenement est obligatoire.")
        LearningEventType type,

        @DecimalMin(value = "0.0", message = "Le score va de 0 a 1.")
        @DecimalMax(value = "1.0", message = "Le score va de 0 a 1.")
        double score,

        double weight,

        String sourceCode,

        Integer durationSeconds,

        Instant occurredAt) {

    public LearningEvent toDomain() {
        return new LearningEvent(null, null, systemCode, notionCode, type, score, weight,
                sourceCode, durationSeconds, occurredAt);
    }
}
