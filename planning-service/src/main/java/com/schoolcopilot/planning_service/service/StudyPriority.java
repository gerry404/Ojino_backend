package com.schoolcopilot.planning_service.service;

import com.schoolcopilot.planning_service.domain.SessionReason;

/**
 * Une notion a travailler, avec son urgence.
 *
 * <p>C'est la monnaie commune du planificateur : echeances, lacunes et revisions
 * espacees se ramenent toutes a cette forme, ce qui permet de les classer les
 * unes par rapport aux autres au lieu de les traiter separement.
 *
 * @param weight plus il est eleve, plus la notion passe tot dans la semaine
 * @param deadlineId renseigne quand la priorite vient d'une echeance
 */
public record StudyPriority(
        String notionCode,
        SessionReason reason,
        double weight,
        String deadlineId) {

    public static StudyPriority of(String notionCode, SessionReason reason, double weight) {
        return new StudyPriority(notionCode, reason, weight, null);
    }

    public static StudyPriority forDeadline(String notionCode, double weight, String deadlineId) {
        return new StudyPriority(notionCode, SessionReason.DEADLINE, weight, deadlineId);
    }
}
