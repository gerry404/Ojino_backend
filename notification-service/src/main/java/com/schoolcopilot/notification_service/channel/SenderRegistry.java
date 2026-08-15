package com.schoolcopilot.notification_service.channel;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.schoolcopilot.notification_service.domain.NotificationChannel;

/**
 * L'annuaire des canaux disponibles.
 *
 * <p>Se remplit tout seul : Spring injecte tous les {@link NotificationSender}
 * declares. Un canal sans expediteur est simplement absent, et les notifications
 * qui le visaient sont ecartees plutot que de rester en file indefiniment.
 */
@Component
public class SenderRegistry {

    private final Map<NotificationChannel, NotificationSender> byChannel =
            new EnumMap<>(NotificationChannel.class);

    public SenderRegistry(List<NotificationSender> senders) {
        senders.forEach(sender -> byChannel.put(sender.channel(), sender));
    }

    public Optional<NotificationSender> forChannel(NotificationChannel channel) {
        return Optional.ofNullable(byChannel.get(channel));
    }

    public boolean supports(NotificationChannel channel) {
        return byChannel.containsKey(channel);
    }
}
