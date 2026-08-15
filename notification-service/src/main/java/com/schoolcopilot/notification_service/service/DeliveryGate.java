package com.schoolcopilot.notification_service.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import com.schoolcopilot.notification_service.domain.NotificationChannel;
import com.schoolcopilot.notification_service.domain.NotificationPreferences;
import com.schoolcopilot.notification_service.domain.NotificationType;

/**
 * Decide si une notification part, attend, ou ne part pas du tout.
 *
 * <p>Logique pure, isolee a dessein : c'est la partie qui protege l'utilisateur,
 * et elle doit pouvoir se lire et se verifier sans base ni reseau. Trois regles se
 * combinent — le consentement, les heures de silence, le plafond quotidien — et
 * leur ordre compte.
 */
@Component
public class DeliveryGate {

    /** Ce qu'il advient d'une notification soumise a la porte. */
    public sealed interface Decision {

        /** A envoyer maintenant. */
        record SendNow() implements Decision {
        }

        /** A envoyer plus tard : tombee dans les heures de silence. */
        record Defer(Instant until, String reason) implements Decision {
        }

        /** A ne pas envoyer du tout. */
        record Suppress(String reason) implements Decision {
        }
    }

    /**
     * @param sentToday nombre de notifications intrusives deja parties aujourd'hui
     */
    public Decision evaluate(NotificationType type, NotificationChannel channel,
            NotificationPreferences preferences, Instant now, int sentToday) {

        // 1. Le consentement d'abord. Inutile de calculer un report pour quelque
        //    chose que l'utilisateur ne veut pas recevoir.
        if (!preferences.accepts(type, channel)) {
            return new Decision.Suppress("canal desactive pour ce type");
        }

        // 2. Le canal in-app ne derange personne : ni silence ni plafond.
        if (!channel.isIntrusive()) {
            return new Decision.SendNow();
        }

        // 3. Une alerte de securite passe outre le reste. Prevenir d'une intrusion
        //    le lendemain matin n'a plus d'interet.
        if (type.urgency() == NotificationType.Urgency.CRITICAL) {
            return new Decision.SendNow();
        }

        // 4. Le plafond avant le report : reporter une notification qui sera de
        //    toute facon ecartee reviendrait a la garder en file pour rien.
        if (sentToday >= preferences.dailyCap()) {
            return new Decision.Suppress("plafond quotidien atteint");
        }

        // 5. Les heures de silence. La notification est reportee, jamais jetee :
        //    un rappel de seance perdu, c'est une seance manquee.
        ZoneId zone = preferences.zone();
        LocalDateTime local = LocalDateTime.ofInstant(now, zone);

        if (preferences.quietHours().covers(local.toLocalTime())) {
            LocalDateTime allowed = preferences.quietHours().nextAllowed(local);
            return new Decision.Defer(allowed.atZone(zone).toInstant(), "heures de silence");
        }

        return new Decision.SendNow();
    }
}
