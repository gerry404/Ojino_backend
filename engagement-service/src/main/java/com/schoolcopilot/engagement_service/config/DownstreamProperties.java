package com.schoolcopilot.engagement_service.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Les services appeles par celui-ci.
 *
 * @param internalToken secret partage des routes internes du service de
 *        notification. Meme reserve qu'ailleurs : il ne distingue pas les
 *        services, et devra ceder la place a une vraie identite de service.
 */
@ConfigurationProperties(prefix = "ojino.downstream")
public record DownstreamProperties(Notification notification) {

    public record Notification(String baseUrl, String internalToken, Duration connectTimeout,
            Duration readTimeout) {
    }
}
