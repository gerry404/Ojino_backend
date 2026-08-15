package com.schoolcopilot.notification_service.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.schoolcopilot.notification_service.channel.NotificationSender;
import com.schoolcopilot.notification_service.channel.SenderRegistry;
import com.schoolcopilot.notification_service.config.NotificationProperties;
import com.schoolcopilot.notification_service.domain.Notification;
import com.schoolcopilot.notification_service.domain.NotificationChannel;
import com.schoolcopilot.notification_service.domain.NotificationPreferences;
import com.schoolcopilot.notification_service.domain.NotificationStatus;
import com.schoolcopilot.notification_service.domain.NotificationType;
import com.schoolcopilot.notification_service.repository.NotificationRepositories;

/**
 * L'envoi des notifications.
 *
 * <p>Rien ne part au moment de la demande : tout passe par une file. Un appelant
 * qui declenche un rappel ne doit pas attendre que FCM reponde, ni echouer parce
 * que FCM est en panne.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepositories.Notifications notifications;
    private final NotificationRepositories.Preferences preferences;
    private final TemplateRenderer templates;
    private final DeliveryGate gate;
    private final SenderRegistry senders;
    private final NotificationProperties properties;

    public NotificationService(NotificationRepositories.Notifications notifications,
            NotificationRepositories.Preferences preferences, TemplateRenderer templates,
            DeliveryGate gate, SenderRegistry senders, NotificationProperties properties) {
        this.notifications = notifications;
        this.preferences = preferences;
        this.templates = templates;
        this.gate = gate;
        this.senders = senders;
        this.properties = properties;
    }

    // ------------------------------------------------------------------
    // Emission
    // ------------------------------------------------------------------

    /**
     * Met une notification en file, sur tous les canaux retenus par
     * l'utilisateur.
     *
     * @param dedupeKey facultatif, mais fortement conseille pour tout ce qui est
     *        declenche par un ordonnanceur : sans lui, un rappel rejoue produit
     *        deux vibrations.
     */
    public List<Notification> enqueue(String userId, NotificationType type,
            Map<String, String> values, String dedupeKey) {

        NotificationPreferences prefs = preferencesOf(userId);
        TemplateRenderer.Rendered text = templates.render(type, prefs.language(), values);
        Instant now = Instant.now();
        int sentToday = countSentToday(userId, prefs.zone());

        List<Notification> created = new ArrayList<>();

        for (NotificationChannel channel : NotificationChannel.values()) {
            if (!senders.supports(channel)) {
                continue;
            }

            DeliveryGate.Decision decision =
                    gate.evaluate(type, channel, prefs, now, sentToday);

            Notification notification = switch (decision) {
                case DeliveryGate.Decision.SendNow ignored ->
                        pending(userId, type, channel, text, values, dedupeKey, now, now);
                case DeliveryGate.Decision.Defer defer -> {
                    log.debug("{} pour {} reportee a {} ({})", type, userId, defer.until(),
                            defer.reason());
                    yield pending(userId, type, channel, text, values, dedupeKey, now,
                            defer.until());
                }
                case DeliveryGate.Decision.Suppress suppress -> suppressed(userId, type, channel,
                        text, values, dedupeKey, now, suppress.reason());
            };

            save(notification).ifPresent(created::add);
        }

        return created;
    }

    // ------------------------------------------------------------------
    // Remise
    // ------------------------------------------------------------------

    /**
     * Traite un lot de notifications dues.
     *
     * <p>Par lots plutot que d'un bloc : une file qui a pris du retard ne doit pas
     * monopoliser la memoire ni bloquer le service pendant qu'elle se vide.
     *
     * @return le nombre de notifications traitees
     */
    public int processQueue() {
        List<Notification> due = notifications
                .findByStatusAndSendAfterLessThanEqualOrderBySendAfterAsc(
                        NotificationStatus.PENDING, Instant.now(),
                        PageRequest.of(0, properties.batchSize()));

        due.forEach(this::deliver);
        return due.size();
    }

    private void deliver(Notification notification) {
        NotificationSender sender = senders.forChannel(notification.channel()).orElse(null);

        if (sender == null) {
            // Le canal a disparu depuis la mise en file : inutile de la garder.
            notifications.save(withStatus(notification, NotificationStatus.SUPPRESSED,
                    "canal indisponible"));
            return;
        }

        try {
            sender.send(notification);
            notifications.save(sent(notification));

        } catch (NotificationSender.DeliveryException e) {
            handleFailure(notification, e.getMessage(), e.isPermanent());

        } catch (RuntimeException e) {
            // Une panne imprevue est traitee comme temporaire : on ne peut pas
            // savoir, et abandonner trop vite fait perdre des notifications.
            handleFailure(notification, e.getMessage(), false);
        }
    }

    /**
     * Reessaie avec un delai croissant.
     *
     * <p>Un delai fixe ferait marteler un service deja en difficulte. Le delai
     * double a chaque tentative, ce qui laisse le temps a une panne passagere de
     * se resorber.
     */
    private void handleFailure(Notification notification, String error, boolean permanent) {
        int attempts = notification.attempts() + 1;

        if (permanent || attempts >= properties.maxAttempts()) {
            log.warn("Notification {} abandonnee apres {} tentatives : {}", notification.id(),
                    attempts, error);
            notifications.save(new Notification(notification.id(), notification.userId(),
                    notification.type(), notification.channel(), notification.title(),
                    notification.body(), notification.data(), notification.dedupeKey(),
                    NotificationStatus.FAILED, notification.sendAfter(), attempts, error,
                    notification.createdAt(), null, null));
            return;
        }

        Duration backoff = properties.retryDelay().multipliedBy(1L << (attempts - 1));

        notifications.save(new Notification(notification.id(), notification.userId(),
                notification.type(), notification.channel(), notification.title(),
                notification.body(), notification.data(), notification.dedupeKey(),
                NotificationStatus.PENDING, Instant.now().plus(backoff), attempts, error,
                notification.createdAt(), null, null));
    }

    // ------------------------------------------------------------------
    // Consultation
    // ------------------------------------------------------------------

    public List<Notification> inbox(String userId) {
        return notifications.findByUserIdAndChannelAndReadAtIsNullOrderByCreatedAtDesc(userId,
                NotificationChannel.IN_APP);
    }

    public long unreadCount(String userId) {
        return notifications.countByUserIdAndChannelAndReadAtIsNull(userId,
                NotificationChannel.IN_APP);
    }

    public List<Notification> history(String userId, int limit) {
        return notifications.findByUserIdOrderByCreatedAtDesc(userId,
                PageRequest.of(0, Math.min(limit, 100)));
    }

    public Notification markRead(String userId, String notificationId) {
        Notification notification = notifications.findById(notificationId)
                .filter(item -> item.userId().equals(userId))
                .orElseThrow(() -> new com.schoolcopilot.notification_service.exception.ApiException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "notification_not_found",
                        "Notification introuvable."));

        return notifications.save(new Notification(notification.id(), notification.userId(),
                notification.type(), notification.channel(), notification.title(),
                notification.body(), notification.data(), notification.dedupeKey(),
                notification.status(), notification.sendAfter(), notification.attempts(),
                notification.lastError(), notification.createdAt(), notification.sentAt(),
                Instant.now()));
    }

    // ------------------------------------------------------------------
    // Preferences
    // ------------------------------------------------------------------

    public NotificationPreferences preferencesOf(String userId) {
        return preferences.findById(userId)
                .orElseGet(() -> NotificationPreferences.defaults(userId));
    }

    public NotificationPreferences updatePreferences(String userId,
            NotificationPreferences changes) {

        NotificationPreferences updated = new NotificationPreferences(userId, changes.zone(),
                changes.language(), changes.quietHours(), changes.channelsByType(),
                Math.max(1, changes.dailyCap()), Instant.now());

        return preferences.save(updated);
    }

    // ------------------------------------------------------------------

    /** Le plafond se compte sur la journee locale de l'utilisateur, pas celle du serveur. */
    private int countSentToday(String userId, ZoneId zone) {
        Instant startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant();

        return (int) notifications.countByUserIdAndStatusAndChannelNotAndSentAtAfter(userId,
                NotificationStatus.SENT, NotificationChannel.IN_APP, startOfDay);
    }

    /**
     * L'index unique sur la cle de deduplication tranche les courses.
     *
     * <p>Deux instances qui traitent le meme declencheur au meme instant passeront
     * toutes deux le test d'existence ; seule l'une des deux ecrira.
     */
    private java.util.Optional<Notification> save(Notification notification) {
        try {
            return java.util.Optional.of(notifications.save(notification));
        } catch (DuplicateKeyException e) {
            log.debug("Notification {} deja en file pour {}, ignoree.", notification.type(),
                    notification.userId());
            return java.util.Optional.empty();
        }
    }

    private Notification pending(String userId, NotificationType type,
            NotificationChannel channel, TemplateRenderer.Rendered text,
            Map<String, String> values, String dedupeKey, Instant now, Instant sendAfter) {

        return new Notification(null, userId, type, channel, text.title(), text.body(), values,
                scopedKey(dedupeKey, channel), NotificationStatus.PENDING, sendAfter, 0, null,
                now, null, null);
    }

    private Notification suppressed(String userId, NotificationType type,
            NotificationChannel channel, TemplateRenderer.Rendered text,
            Map<String, String> values, String dedupeKey, Instant now, String reason) {

        return new Notification(null, userId, type, channel, text.title(), text.body(), values,
                scopedKey(dedupeKey, channel), NotificationStatus.SUPPRESSED, now, 0, reason, now,
                null, null);
    }

    /**
     * La cle est portee par canal.
     *
     * <p>Sans cela, la version push et la version in-app de la meme information
     * entreraient en collision sur l'index unique, et une seule des deux
     * survivrait.
     */
    private String scopedKey(String dedupeKey, NotificationChannel channel) {
        return dedupeKey == null ? null : dedupeKey + ":" + channel;
    }

    private Notification sent(Notification notification) {
        return new Notification(notification.id(), notification.userId(), notification.type(),
                notification.channel(), notification.title(), notification.body(),
                notification.data(), notification.dedupeKey(), NotificationStatus.SENT,
                notification.sendAfter(), notification.attempts() + 1, null,
                notification.createdAt(), Instant.now(), null);
    }

    private Notification withStatus(Notification notification, NotificationStatus status,
            String error) {
        return new Notification(notification.id(), notification.userId(), notification.type(),
                notification.channel(), notification.title(), notification.body(),
                notification.data(), notification.dedupeKey(), status, notification.sendAfter(),
                notification.attempts(), error, notification.createdAt(), null, null);
    }

    /** Les canaux servis, exposes pour les tests et le diagnostic. */
    public Set<NotificationChannel> availableChannels() {
        return java.util.Arrays.stream(NotificationChannel.values())
                .filter(senders::supports)
                .collect(java.util.stream.Collectors.toCollection(
                        () -> java.util.EnumSet.noneOf(NotificationChannel.class)));
    }
}
