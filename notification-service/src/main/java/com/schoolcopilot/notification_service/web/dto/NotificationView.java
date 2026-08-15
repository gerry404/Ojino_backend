package com.schoolcopilot.notification_service.web.dto;

import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.schoolcopilot.notification_service.domain.Notification;
import com.schoolcopilot.notification_service.domain.NotificationChannel;
import com.schoolcopilot.notification_service.domain.NotificationStatus;
import com.schoolcopilot.notification_service.domain.NotificationType;

/**
 * Une notification telle que les applications la voient.
 *
 * <p>Ni la cle de deduplication ni le detail des erreurs n'y figurent : ce sont
 * des rouages internes, sans interet pour l'utilisateur.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record NotificationView(
        String id,
        NotificationType type,
        NotificationChannel channel,
        String title,
        String body,
        Map<String, String> data,
        NotificationStatus status,
        Instant createdAt,
        Instant readAt) {

    public static NotificationView from(Notification notification) {
        return new NotificationView(notification.id(), notification.type(),
                notification.channel(), notification.title(), notification.body(),
                notification.data(), notification.status(), notification.createdAt(),
                notification.readAt());
    }
}
