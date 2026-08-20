package com.schoolcopilot.assistant_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.schoolcopilot.assistant_service.engine.AiEngine;
import com.schoolcopilot.assistant_service.engine.CannedAiEngine;
import com.schoolcopilot.assistant_service.engine.RemoteAiEngine;

@Configuration
public class EngineConfig {

    private static final Logger log = LoggerFactory.getLogger(EngineConfig.class);

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

    @Bean
    @ConditionalOnProperty(name = "ojino.assistant.engine", havingValue = "remote")
    AiEngine remoteAiEngine(RestClient aiRestClient) {
        log.info("moteur d'inference distant actif");
        return new RemoteAiEngine(aiRestClient);
    }

    /**
     * Le client vers ai-service.
     *
     * <p>Il ne rejoint pas {@code DownstreamClientConfig} pour une raison de
     * fond : les trois services consultes pour construire le contexte doivent
     * repondre en quelques dizaines de millisecondes, un modele met plusieurs
     * secondes. Les memes delais des deux cotes couperaient chaque question.
     *
     * <p>Le jeton interne est pose une fois pour toutes ici. Le repeter a chaque
     * appel finirait par etre oublie quelque part, et l'oubli ne se verrait qu'en
     * production, sous la forme d'un 401.
     */
    @Bean
    @ConditionalOnProperty(name = "ojino.assistant.engine", havingValue = "remote")
    RestClient aiRestClient(AiEngineProperties properties) {
        if (properties.internalToken() == null || properties.internalToken().isBlank()) {
            // Echouer au demarrage plutot qu'a la premiere question : le probleme
            // se voit au deploiement, pas devant un eleve.
            throw new IllegalStateException(
                    "ojino.assistant.ai.internal-token est obligatoire quand "
                            + "ojino.assistant.engine=remote");
        }

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .defaultHeader("X-Internal-Token", properties.internalToken())
                .build();
    }
}
