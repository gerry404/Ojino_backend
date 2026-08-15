package com.schoolcopilot.notification_service.domain;

import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Ce que l'utilisateur accepte de recevoir, et quand.
 *
 * <p>{@code zone} est indispensable : l'heure de silence est une heure locale, et
 * le serveur ne vit pas forcement dans le meme fuseau que l'eleve. Sans elle, un
 * eleve au Cameroun serait derange selon l'horloge d'un serveur europeen.
 *
 * @param channelsByType canaux retenus par type. Un type absent signifie "reglage
 *        par defaut", pas "coupe" — sinon ajouter un nouveau type le rendrait
 *        muet pour tous les comptes existants.
 */
@Document(collection = "notification_preferences")
public record NotificationPreferences(
        @Id String userId,
        ZoneId zone,
        String language,
        QuietHours quietHours,
        Map<NotificationType, Set<NotificationChannel>> channelsByType,
        int dailyCap,
        Instant updatedAt) {

    /** Tous les canaux, hors application, restent intrusifs et donc plafonnes. */
    private static final Set<NotificationChannel> DEFAULT_CHANNELS =
            EnumSet.of(NotificationChannel.PUSH, NotificationChannel.IN_APP);

    public static NotificationPreferences defaults(String userId) {
        return new NotificationPreferences(userId, ZoneId.of("Africa/Douala"), "fr",
                QuietHours.defaults(), new EnumMap<>(NotificationType.class), 6, Instant.now());
    }

    public Map<NotificationType, Set<NotificationChannel>> channelsByType() {
        return channelsByType == null ? Map.of() : channelsByType;
    }

    /**
     * Les canaux retenus pour ce type.
     *
     * <p>Une alerte de securite ne se coupe pas : elle ressort quoi qu'il arrive.
     * Laisser quelqu'un desactiver l'avertissement d'une intrusion serait lui
     * rendre un mauvais service.
     */
    public Set<NotificationChannel> channelsFor(NotificationType type) {
        if (!type.isOptOutAllowed()) {
            return EnumSet.of(NotificationChannel.PUSH, NotificationChannel.EMAIL,
                    NotificationChannel.IN_APP);
        }
        Set<NotificationChannel> configured = channelsByType().get(type);
        return configured == null ? DEFAULT_CHANNELS : configured;
    }

    public boolean accepts(NotificationType type, NotificationChannel channel) {
        return channelsFor(type).contains(channel);
    }
}
