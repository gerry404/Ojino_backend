package com.schoolcopilot.planning_service.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Une echeance : devoir a rendre, controle, examen officiel.
 *
 * <p>C'est ce qui donne une direction au planning. Sans echeance, reviser n'a pas
 * d'urgence relative et toutes les notions se valent ; avec elle, ce qui tombe la
 * semaine prochaine passe devant.
 *
 * @param notionCodes ce que l'echeance couvre. Vide vaut "toute la matiere" — le
 *        cas d'un examen de fin d'annee.
 * @param importance de 1 a 5. Un bac ne se prepare pas comme une interrogation
 *        surprise, et le planificateur doit pouvoir arbitrer entre deux echeances
 *        qui tombent le meme jour.
 */
@Document(collection = "deadlines")
@CompoundIndex(name = "idx_deadline_user_date", def = "{'userId': 1, 'dueOn': 1}")
public record Deadline(
        @Id String id,
        @Indexed String userId,
        String systemCode,
        DeadlineType type,
        String label,
        String anchorCode,
        List<String> notionCodes,
        LocalDate dueOn,
        int importance,
        boolean completed,
        Instant createdAt) {

    public List<String> notionCodes() {
        return notionCodes == null ? List.of() : notionCodes;
    }

    /** Negatif si l'echeance est passee. */
    public long daysUntil(LocalDate today) {
        return ChronoUnit.DAYS.between(today, dueOn);
    }

    public boolean isUpcoming(LocalDate today) {
        return !completed && !dueOn.isBefore(today);
    }

    /**
     * L'urgence combine importance et proximite.
     *
     * <p>Une echeance lointaine mais capitale doit finir par remonter, et une
     * echeance mineure de demain ne doit pas ecraser tout le reste. Le
     * denominateur evite la division par zero le jour meme.
     */
    public double urgency(LocalDate today) {
        long days = Math.max(daysUntil(today), 0);
        return importance / (1.0 + days);
    }
}
