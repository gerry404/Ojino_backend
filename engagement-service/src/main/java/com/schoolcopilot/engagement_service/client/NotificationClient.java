package com.schoolcopilot.engagement_service.client;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.schoolcopilot.engagement_service.config.DownstreamProperties;

/**
 * Les relances passent par {@code notification-service}.
 *
 * <p>Ce service ne decide ni du canal, ni de l'heure, ni du plafond : il dit
 * seulement qu'il y a quelque chose a annoncer. Les heures de silence et le
 * consentement de l'utilisateur sont l'affaire du service de notification, et lui
 * seul.
 *
 * <p>Un echec n'est jamais propage : rater une relance de motivation est sans
 * gravite, faire echouer l'enregistrement d'une activite parce qu'une relance n'a
 * pas pu partir serait absurde.
 */
@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final RestClient restClient;
    private final DownstreamProperties properties;

    public NotificationClient(RestClient notificationRestClient,
            DownstreamProperties properties) {
        this.restClient = notificationRestClient;
        this.properties = properties;
    }

    /**
     * @param dedupeKey empeche la relance en double si la tache quotidienne est
     *        rejouee
     */
    public void notify(String userId, String type, Map<String, String> values,
            String dedupeKey) {
        try {
            restClient.post()
                    .uri("/api/v1/internal/notifications")
                    .header("X-Internal-Token", properties.notification().internalToken())
                    .body(Map.of(
                            "userId", userId,
                            "type", type,
                            "values", values,
                            "dedupeKey", dedupeKey))
                    .retrieve()
                    .toBodilessEntity();

        } catch (RuntimeException e) {
            log.warn("Relance {} non transmise pour {} : {}", type, userId, e.getMessage());
        }
    }
}
