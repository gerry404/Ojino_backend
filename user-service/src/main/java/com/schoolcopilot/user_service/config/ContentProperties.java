package com.schoolcopilot.user_service.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Acces au content-service, qui detient le referentiel scolaire.
 *
 * <p>Les delais sont volontairement courts : ce service n'est appele que pour
 * valider un choix pendant l'inscription. Mieux vaut echouer vite et dire a
 * l'eleve de reessayer que de laisser sa requete pendre.
 */
@ConfigurationProperties(prefix = "ojino.content")
public record ContentProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {
}
