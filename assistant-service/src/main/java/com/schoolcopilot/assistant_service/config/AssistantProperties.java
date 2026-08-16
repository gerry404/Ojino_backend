package com.schoolcopilot.assistant_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reglages de l'assistant.
 *
 * <p>Les quotas sont externalises pour une raison simple : ils arbitrent un cout
 * reel, et le bon reglage ne se decide pas a l'avance. Il se mesure.
 */
@ConfigurationProperties(prefix = "ojino.assistant")
public record AssistantProperties(String engine, Quota quota, Context context) {

    /**
     * @param dailyMessages nombre de questions par jour
     * @param dailyTokens jetons par jour. Compter les messages ne suffit pas :
     *        quelqu'un qui colle trois pages de cours consomme cent fois plus
     *        qu'une question courte.
     * @param maxInputChars taille maximale d'une question, refusee avant tout
     *        appel
     */
    public record Quota(int dailyMessages, int dailyTokens, int maxInputChars) {
    }

    /**
     * @param maxTurns nombre de tours de conversation renvoyes au moteur
     * @param maxHistoryChars volume total de l'historique. Se combine avec
     *        {@code maxTurns} : sans lui, quelques messages tres longs
     *        suffiraient a tout saturer.
     */
    public record Context(int maxTurns, int maxHistoryChars) {
    }
}
