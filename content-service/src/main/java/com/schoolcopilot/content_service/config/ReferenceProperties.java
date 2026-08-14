package com.schoolcopilot.content_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param seedOnStartup charge les systemes livres avec l'application si la base
 *        est vide. Ne reecrit jamais des donnees existantes.
 * @param defaultSystem systeme propose quand le client n'en precise aucun
 */
@ConfigurationProperties(prefix = "ojino.reference")
public record ReferenceProperties(boolean seedOnStartup, String defaultSystem) {
}
