package com.schoolcopilot.notification_service.channel;

import com.schoolcopilot.notification_service.domain.Notification;
import com.schoolcopilot.notification_service.domain.NotificationChannel;

/**
 * La remise effective sur un canal.
 *
 * <p>Abstrait comme {@code SmsSender} dans l'auth-service : brancher FCM ou un
 * fournisseur d'emails revient a declarer un bean de plus. Le reste du service ne
 * sait pas comment une notification part.
 */
public interface NotificationSender {

    NotificationChannel channel();

    /**
     * @throws DeliveryException si la remise echoue de facon temporaire — la
     *         notification sera reessayee
     */
    void send(Notification notification);

    /**
     * Echec de remise.
     *
     * @param permanent vrai si reessayer ne servira a rien : jeton de push
     *        revoque, adresse email inexistante. On abandonne alors tout de suite
     *        au lieu d'user les tentatives pour rien.
     */
    class DeliveryException extends RuntimeException {

        private final boolean permanent;

        public DeliveryException(String message, boolean permanent) {
            super(message);
            this.permanent = permanent;
        }

        public boolean isPermanent() {
            return permanent;
        }
    }
}
