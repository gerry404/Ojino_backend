package com.schoolcopilot.notification_service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.schoolcopilot.notification_service.channel.SenderRegistry;
import com.schoolcopilot.notification_service.domain.NotificationChannel;
import com.schoolcopilot.notification_service.repository.NotificationRepositories;
import com.schoolcopilot.notification_service.service.NotificationService;
import com.schoolcopilot.notification_service.service.TemplateRenderer;
import com.schoolcopilot.notification_service.web.InternalNotificationController;
import com.schoolcopilot.notification_service.web.NotificationController;

/**
 * Verifie le cablage complet : securite, filtre interne, expediteurs, gabarits.
 *
 * <p>La lecture des gabarits se fait a la construction : ce test attrape donc
 * aussi un fichier JSON malforme, avant qu'il n'atteigne la production.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration"
})
class NotificationServiceApplicationTests {

    @MockitoBean
    NotificationRepositories.Notifications notifications;

    @MockitoBean
    NotificationRepositories.Preferences preferences;

    @Autowired
    NotificationService notificationService;

    @Autowired
    NotificationController notificationController;

    @Autowired
    InternalNotificationController internalController;

    @Autowired
    SenderRegistry senders;

    @Autowired
    TemplateRenderer templates;

    @Test
    void contextLoads() {
        assertThat(notificationService).isNotNull();
        assertThat(notificationController).isNotNull();
        assertThat(internalController).isNotNull();
    }

    @Test
    void everyChannelHasASender() {
        // Un canal sans expediteur verrait ses notifications ecartees en silence.
        assertThat(senders.supports(NotificationChannel.PUSH)).isTrue();
        assertThat(senders.supports(NotificationChannel.EMAIL)).isTrue();
        assertThat(senders.supports(NotificationChannel.IN_APP)).isTrue();
    }

    @Test
    void everyTypeHasATemplateInBothLanguages() {
        // Un type sans gabarit produirait une notification sans texte, ce qui ne
        // se verrait qu'une fois envoyee.
        for (var type : com.schoolcopilot.notification_service.domain.NotificationType.values()) {
            assertThat(templates.render(type, "fr", java.util.Map.of()).title())
                    .as("gabarit francais pour %s", type)
                    .isNotEqualTo(type.name());
            assertThat(templates.render(type, "en", java.util.Map.of()).title())
                    .as("gabarit anglais pour %s", type)
                    .isNotEqualTo(type.name());
        }
    }
}
