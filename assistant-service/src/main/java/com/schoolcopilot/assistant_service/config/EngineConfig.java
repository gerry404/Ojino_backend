package com.schoolcopilot.assistant_service.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.schoolcopilot.assistant_service.engine.AiEngine;
import com.schoolcopilot.assistant_service.engine.CannedAiEngine;

@Configuration
public class EngineConfig {

    /**
     * Le moteur bouchonne, actif tant qu'aucun autre n'est choisi.
     *
     * <p>La condition porte sur une propriete et non sur l'absence d'un autre
     * bean : {@code @ConditionalOnMissingBean} depend de l'ordre de traitement des
     * configurations et ne vaut de facon fiable qu'en autoconfiguration. Le choix
     * du moteur doit rester explicite.
     */
    @Bean
    @ConditionalOnProperty(name = "ojino.assistant.engine", havingValue = "canned",
            matchIfMissing = true)
    AiEngine cannedAiEngine() {
        return new CannedAiEngine();
    }
}
