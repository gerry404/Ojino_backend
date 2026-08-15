package com.schoolcopilot.notification_service.domain;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Une notification a envoyer, ou deja envoyee.
 *
 * <p>Un document par couple destinataire / canal : la meme information peut partir
 * en push et rester consultable dans l'application, avec des sorts differents si
 * l'un des deux echoue.
 *
 * @param dedupeKey empeche d'envoyer deux fois la meme chose. Un rappel de seance
 *        rejoue par un ordonnanceur ne doit pas se traduire par deux vibrations.
 * @param attempts nombre de tentatives d'envoi deja faites
 */
@Document(collection = "notifications")
@CompoundIndex(name = "idx_notification_queue", def = "{'status': 1, 'sendAfter': 1}")
@CompoundIndex(name = "idx_notification_user", def = "{'userId': 1, 'createdAt': -1}")
@CompoundIndex(name = "idx_notification_dedupe", def = "{'dedupeKey': 1}", unique = true,
        sparse = true)
public record Notification(
        @Id String id,
        @Indexed String userId,
        NotificationType type,
        NotificationChannel channel,
        String title,
        String body,
        Map<String, String> data,
        String dedupeKey,
        NotificationStatus status,
        Instant sendAfter,
        int attempts,
        String lastError,
        Instant createdAt,
        Instant sentAt,
        Instant readAt) {

    public Map<String, String> data() {
        return data == null ? Map.of() : data;
    }

    public boolean isPending() {
        return status == NotificationStatus.PENDING;
    }

    public boolean isDue(Instant now) {
        return isPending() && !sendAfter.isAfter(now);
    }
}
