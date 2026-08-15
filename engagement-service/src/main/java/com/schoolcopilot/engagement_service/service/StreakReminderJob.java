package com.schoolcopilot.engagement_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Previent en fin de journee ceux dont la serie va se rompre.
 *
 * <p>L'heure est configurable parce qu'elle est un compromis : trop tot, la
 * relance arrive avant que la journee ait eu sa chance ; trop tard, elle tombe
 * dans les heures de silence et sera reportee au lendemain, quand il sera trop
 * tard.
 *
 * <p>Le service de notification reste seul juge de l'envoi effectif : il
 * appliquera les preferences, le plafond et les heures de silence.
 */
@Component
public class StreakReminderJob {

    private final EngagementService engagement;

    public StreakReminderJob(EngagementService engagement) {
        this.engagement = engagement;
    }

    @Scheduled(cron = "${ojino.engagement.streak-reminder-cron:0 0 19 * * *}")
    public void remind() {
        engagement.notifyStreaksAtRisk();
    }
}
