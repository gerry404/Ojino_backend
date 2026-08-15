package com.schoolcopilot.notification_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schoolcopilot.notification_service.domain.NotificationChannel;
import com.schoolcopilot.notification_service.domain.NotificationPreferences;
import com.schoolcopilot.notification_service.domain.NotificationType;
import com.schoolcopilot.notification_service.domain.QuietHours;

/**
 * La porte de sortie : consentement, heures de silence, plafond. Leur ordre
 * compte autant que leur contenu.
 */
class DeliveryGateTest {

    private static final ZoneId DOUALA = ZoneId.of("Africa/Douala");

    private final DeliveryGate gate = new DeliveryGate();

    @Test
    @DisplayName("en journee, une notification ordinaire part tout de suite")
    void routineGoesOutDuringTheDay() {
        assertThat(gate.evaluate(NotificationType.SESSION_REMINDER, NotificationChannel.PUSH,
                preferences(), at(15, 0), 0))
                .isInstanceOf(DeliveryGate.Decision.SendNow.class);
    }

    @Test
    @DisplayName("le soir, elle est reportee et non jetee")
    void routineIsDeferredAtNight() {
        DeliveryGate.Decision decision = gate.evaluate(NotificationType.SESSION_REMINDER,
                NotificationChannel.PUSH, preferences(), at(23, 0), 0);

        // Un rappel de seance perdu, c'est une seance manquee.
        assertThat(decision).isInstanceOf(DeliveryGate.Decision.Defer.class);
    }

    @Test
    @DisplayName("une alerte de securite traverse les heures de silence")
    void securityAlertIgnoresQuietHours() {
        // Prevenir d'une intrusion le lendemain matin n'a plus d'interet.
        assertThat(gate.evaluate(NotificationType.SECURITY_ALERT, NotificationChannel.PUSH,
                preferences(), at(3, 0), 0))
                .isInstanceOf(DeliveryGate.Decision.SendNow.class);
    }

    @Test
    @DisplayName("une alerte de securite passe aussi outre le plafond")
    void securityAlertIgnoresTheDailyCap() {
        assertThat(gate.evaluate(NotificationType.SECURITY_ALERT, NotificationChannel.PUSH,
                preferences(), at(15, 0), 999))
                .isInstanceOf(DeliveryGate.Decision.SendNow.class);
    }

    @Test
    @DisplayName("une alerte de securite ne peut pas etre coupee dans les preferences")
    void securityAlertCannotBeMuted() {
        Map<NotificationType, Set<NotificationChannel>> muted =
                new EnumMap<>(NotificationType.class);
        muted.put(NotificationType.SECURITY_ALERT, EnumSet.noneOf(NotificationChannel.class));

        assertThat(gate.evaluate(NotificationType.SECURITY_ALERT, NotificationChannel.PUSH,
                preferences(muted, 6), at(15, 0), 0))
                .isInstanceOf(DeliveryGate.Decision.SendNow.class);
    }

    @Test
    @DisplayName("un canal coupe pour ce type ecarte la notification")
    void mutedChannelSuppresses() {
        Map<NotificationType, Set<NotificationChannel>> muted =
                new EnumMap<>(NotificationType.class);
        muted.put(NotificationType.ENCOURAGEMENT, EnumSet.of(NotificationChannel.IN_APP));

        assertThat(gate.evaluate(NotificationType.ENCOURAGEMENT, NotificationChannel.PUSH,
                preferences(muted, 6), at(15, 0), 0))
                .isInstanceOf(DeliveryGate.Decision.Suppress.class);
    }

    @Test
    @DisplayName("le plafond quotidien ecarte au lieu de reporter")
    void dailyCapSuppresses() {
        DeliveryGate.Decision decision = gate.evaluate(NotificationType.SESSION_REMINDER,
                NotificationChannel.PUSH, preferences(), at(15, 0), 6);

        // Reporter une notification qui sera de toute facon ecartee reviendrait a
        // la garder en file pour rien.
        assertThat(decision).isInstanceOf(DeliveryGate.Decision.Suppress.class);
    }

    @Test
    @DisplayName("le plafond est evalue avant les heures de silence")
    void capIsCheckedBeforeQuietHours() {
        DeliveryGate.Decision decision = gate.evaluate(NotificationType.SESSION_REMINDER,
                NotificationChannel.PUSH, preferences(), at(23, 0), 6);

        assertThat(decision).isInstanceOf(DeliveryGate.Decision.Suppress.class);
    }

    @Test
    @DisplayName("le canal in-app ignore silence et plafond")
    void inAppIsNeverThrottled() {
        // Il ne derange personne, et une notification doit rester consultable
        // quelque part meme quand tout le reste est coupe.
        assertThat(gate.evaluate(NotificationType.SESSION_REMINDER, NotificationChannel.IN_APP,
                preferences(), at(3, 0), 999))
                .isInstanceOf(DeliveryGate.Decision.SendNow.class);
    }

    @Test
    @DisplayName("le silence suit l'heure locale de l'eleve, pas celle du serveur")
    void quietHoursFollowTheUserTimezone() {
        NotificationPreferences inTokyo = new NotificationPreferences("user-1",
                ZoneId.of("Asia/Tokyo"), "fr", QuietHours.defaults(),
                new EnumMap<>(NotificationType.class), 6, null);

        // 15h a Douala, soit 23h a Tokyo : l'eleve japonais dort.
        DeliveryGate.Decision decision = gate.evaluate(NotificationType.SESSION_REMINDER,
                NotificationChannel.PUSH, inTokyo, at(15, 0), 0);

        assertThat(decision).isInstanceOf(DeliveryGate.Decision.Defer.class);
    }

    private NotificationPreferences preferences() {
        return preferences(new EnumMap<>(NotificationType.class), 6);
    }

    private NotificationPreferences preferences(
            Map<NotificationType, Set<NotificationChannel>> channels, int dailyCap) {
        return new NotificationPreferences("user-1", DOUALA, "fr", QuietHours.defaults(),
                channels, dailyCap, null);
    }

    /** Un instant correspondant a cette heure locale a Douala. */
    private Instant at(int hour, int minute) {
        return LocalDateTime.of(2026, 8, 19, 0, 0)
                .with(LocalTime.of(hour, minute))
                .atZone(DOUALA)
                .toInstant();
    }
}
