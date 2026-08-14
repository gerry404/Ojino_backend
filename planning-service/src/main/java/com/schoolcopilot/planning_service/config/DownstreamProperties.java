package com.schoolcopilot.planning_service.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Les services dont dependent la generation d'un planning.
 *
 * <p>Les delais sont courts : generer un planning est une action ponctuelle, et
 * mieux vaut echouer vite que laisser la requete de l'eleve pendre.
 */
@ConfigurationProperties(prefix = "ojino.downstream")
public record DownstreamProperties(
        Service user,
        Service learning) {

    public record Service(String baseUrl, Duration connectTimeout, Duration readTimeout) {
    }
}
