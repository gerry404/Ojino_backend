package com.schoolcopilot.notification_service.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.schoolcopilot.notification_service.domain.Notification;
import com.schoolcopilot.notification_service.domain.NotificationChannel;

/**
 * Implementations de developpement : les notifications partent dans les logs.
 *
 * <p>Elles permettent d'eprouver tout le parcours — preferences, heures de
 * silence, plafond, reessais — sans compte FCM ni fournisseur d'emails. Elles
 * s'effacent des qu'un vrai expediteur est declare pour leur canal.
 */
public final class LoggingSenders {

    private static final Logger log = LoggerFactory.getLogger(LoggingSenders.class);

    private LoggingSenders() {
    }

    public static NotificationSender forChannel(NotificationChannel channel) {
        return new NotificationSender() {

            @Override
            public NotificationChannel channel() {
                return channel;
            }

            @Override
            public void send(Notification notification) {
                log.warn("[{} SIMULE] -> {} : {} | {}", channel, notification.userId(),
                        notification.title(), notification.body());
            }
        };
    }

    /**
     * Le canal in-app n'a rien a remettre : la notification <em>est</em> le
     * message, et le client la lira en appelant le service. Son enregistrement
     * suffit.
     */
    public static NotificationSender inApp() {
        return new NotificationSender() {

            @Override
            public NotificationChannel channel() {
                return NotificationChannel.IN_APP;
            }

            @Override
            public void send(Notification notification) {
                // Volontairement vide.
            }
        };
    }
}
