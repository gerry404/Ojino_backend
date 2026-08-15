package com.schoolcopilot.notification_service.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.schoolcopilot.notification_service.channel.LoggingSenders;
import com.schoolcopilot.notification_service.channel.NotificationSender;
import com.schoolcopilot.notification_service.domain.NotificationChannel;

@Configuration
public class ChannelConfig {

    /**
     * Le canal in-app n'a pas de variante simulee : il ne remet rien nulle part,
     * son enregistrement suffit. Il est donc toujours actif.
     */
    @Bean
    NotificationSender inAppSender() {
        return LoggingSenders.inApp();
    }

    /**
     * Expediteurs de developpement.
     *
     * <p>La condition porte sur une propriete, pas sur l'absence d'un autre bean :
     * {@code @ConditionalOnMissingBean} depend de l'ordre de traitement des
     * configurations et ne vaut de facon fiable qu'en autoconfiguration. Le choix
     * doit rester explicite.
     */
    @Bean
    @ConditionalOnProperty(name = "ojino.notification.senders", havingValue = "logging",
            matchIfMissing = true)
    NotificationSender loggingPushSender() {
        return LoggingSenders.forChannel(NotificationChannel.PUSH);
    }

    @Bean
    @ConditionalOnProperty(name = "ojino.notification.senders", havingValue = "logging",
            matchIfMissing = true)
    NotificationSender loggingEmailSender() {
        return LoggingSenders.forChannel(NotificationChannel.EMAIL);
    }
}
