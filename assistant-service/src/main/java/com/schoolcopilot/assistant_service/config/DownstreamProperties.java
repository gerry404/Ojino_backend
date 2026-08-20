package com.schoolcopilot.assistant_service.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Les services consultes pour construire le contexte.
 *
 * <p>Les delais sont courts : trois appels precedent chaque question, et un
 * service lent se paierait sur le temps de reponse de l'assistant.
 */
@ConfigurationProperties(prefix = "ojino.downstream")
public record DownstreamProperties(Service user, Service learning, Service content) {

    public record Service(String baseUrl, Duration connectTimeout, Duration readTimeout) {
    }
}
