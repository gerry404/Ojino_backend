package com.schoolcopilot.learning_service.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.schoolcopilot.learning_service.domain.MasteryLevel;
import com.schoolcopilot.learning_service.service.LearningService;

/**
 * Une etape du parcours de rattrapage.
 *
 * <p>La notion vient du programme, le niveau de maitrise du suivi. C'est
 * precisement le croisement des deux qui a de la valeur : la liste ne contient
 * que ce qui reste a reprendre.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RemediationView(
        String notionCode,
        String label,
        String summary,
        MasteryLevel level,
        Double score) {

    public static RemediationView from(LearningService.Remediation remediation) {
        return new RemediationView(
                remediation.notion().code(),
                remediation.notion().label(),
                remediation.notion().summary(),
                remediation.level(),
                remediation.mastery() == null ? null : remediation.mastery().score());
    }
}
