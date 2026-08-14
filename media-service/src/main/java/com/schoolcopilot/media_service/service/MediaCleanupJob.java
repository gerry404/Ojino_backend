package com.schoolcopilot.media_service.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Nettoie periodiquement les envois jamais confirmes.
 *
 * <p>Toutes les heures : ces demandes ne genent personne a court terme, mais elles
 * s'accumuleraient indefiniment avec les fichiers partiels qui vont avec.
 *
 * <p>Le jour ou plusieurs instances tourneront, il faudra un verrou partage —
 * sinon elles feront le meme travail en parallele. Sans consequence ici, puisque
 * l'operation est idempotente, mais a surveiller.
 */
@Component
public class MediaCleanupJob {

    private final MediaService media;

    public MediaCleanupJob(MediaService media) {
        this.media = media;
    }

    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT5M")
    public void cleanup() {
        media.cleanupAbandoned();
    }
}
