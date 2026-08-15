package com.schoolcopilot.notification_service.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reglages de la file d'envoi.
 *
 * @param batchSize taille d'un lot. Une file en retard ne doit pas monopoliser la
 *        memoire ni bloquer le service pendant qu'elle se vide.
 * @param maxAttempts au-dela, la notification est abandonnee mais conservee
 * @param retryDelay delai avant la premiere reprise. Il double a chaque tentative,
 *        pour ne pas marteler un service deja en difficulte.
 */
@ConfigurationProperties(prefix = "ojino.notification")
public record NotificationProperties(
        int batchSize,
        int maxAttempts,
        Duration retryDelay) {
}
