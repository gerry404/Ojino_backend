package com.schoolcopilot.assistant_service.config;

import java.time.Duration;

/**
 * Le service d'inference, quand le moteur distant est actif.
 *
 * <p>Separe de {@link DownstreamProperties} a dessein : les trois services
 * consultes pour construire le contexte repondent en quelques dizaines de
 * millisecondes, un modele met plusieurs secondes. Les ranger ensemble
 * conduirait tot ou tard quelqu'un a harmoniser les delais, et a couper chaque
 * question au bout de trois secondes.
 *
 * @param internalToken secret partage avec ai-service. Ce service n'est jamais
 *        expose publiquement : c'est un appel de machine a machine, sans jeton
 *        d'utilisateur a transmettre dans ce sens.
 * @param readTimeout genereux par necessite. Une inference longue est normale ;
 *        c'est le seul appel du service ou l'attente n'est pas le signe d'une
 *        panne.
 */
@org.springframework.boot.context.properties.ConfigurationProperties(
        prefix = "ojino.assistant.ai")
public record AiEngineProperties(
        String baseUrl,
        String internalToken,
        Duration connectTimeout,
        Duration readTimeout) {
}
