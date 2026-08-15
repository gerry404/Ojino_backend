package com.schoolcopilot.notification_service.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Vide la file d'envoi.
 *
 * <p>Toutes les trente secondes : assez souvent pour qu'un rappel de seance ne
 * soit pas ressenti comme en retard, assez espace pour ne pas interroger la base
 * en continu.
 *
 * <p>A plusieurs instances, il faudra un verrou partage — sinon elles se
 * disputeront les memes notifications. L'index unique sur la cle de deduplication
 * limite les degats, mais ne remplace pas un verrou.
 */
@Component
public class NotificationDispatchJob {

    private final NotificationService notifications;

    public NotificationDispatchJob(NotificationService notifications) {
        this.notifications = notifications;
    }

    @Scheduled(fixedDelayString = "PT30S", initialDelayString = "PT10S")
    public void dispatch() {
        notifications.processQueue();
    }
}
