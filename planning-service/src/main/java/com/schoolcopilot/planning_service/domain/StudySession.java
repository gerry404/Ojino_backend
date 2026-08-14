package com.schoolcopilot.planning_service.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Une seance de travail planifiee : une notion, un jour, un creneau.
 *
 * <p>Le grain est volontairement la notion et non la matiere. "Reviser les maths
 * mardi de 18h a 19h" ne dit pas quoi faire ; "reprendre les limites" si.
 *
 * @param deadlineId l'echeance qui a motive cette seance, si elle en a une. Une
 *        seance de revision espacee n'en a pas.
 * @param reason pourquoi cette seance a ete proposee — sert a l'expliquer a
 *        l'eleve plutot que de lui imposer un planning opaque.
 */
@Document(collection = "study_sessions")
@CompoundIndex(name = "idx_session_user_date", def = "{'userId': 1, 'scheduledOn': 1}")
@CompoundIndex(name = "idx_session_user_status", def = "{'userId': 1, 'status': 1}")
public record StudySession(
        @Id String id,
        @Indexed String userId,
        String systemCode,
        String notionCode,
        String deadlineId,
        SessionReason reason,
        LocalDate scheduledOn,
        LocalTime startTime,
        LocalTime endTime,
        SessionStatus status,
        Instant startedAt,
        Instant completedAt,
        Integer actualMinutes,
        Instant createdAt) {

    public int plannedMinutes() {
        return (int) Duration.between(startTime, endTime).toMinutes();
    }

    public boolean isOpen() {
        return status == SessionStatus.PLANNED || status == SessionStatus.IN_PROGRESS;
    }

    /**
     * Une seance passee que l'eleve n'a ni commencee ni annulee.
     *
     * <p>Ce sont ces seances-la que la replanification recupere, plutot que de les
     * laisser s'accumuler en reproche silencieux.
     */
    public boolean isMissed(LocalDate today) {
        return status == SessionStatus.PLANNED && scheduledOn.isBefore(today);
    }
}
