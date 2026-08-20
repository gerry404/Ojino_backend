package com.schoolcopilot.assistant_service.client;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Ce sur quoi l'eleve bloque, detenu par {@code learning-service}.
 *
 * <p>Panne <strong>non bloquante</strong> : sans ces lacunes, la reponse sera
 * moins ciblee mais restera utile. Refuser de repondre parce qu'on ignore ou
 * l'eleve bloque serait disproportionne.
 */
@Component
public class LearningClient {

    private static final Logger log = LoggerFactory.getLogger(LearningClient.class);

    private final RestClient restClient;

    public LearningClient(RestClient learningRestClient) {
        this.restClient = learningRestClient;
    }

    public record MasteryView(String notionCode, String level, double score) {
    }

    /** Renvoie une liste vide si le service ne repond pas. */
    public List<MasteryView> gaps(String systemCode, String bearerToken) {
        try {
            List<MasteryView> result = restClient.get()
                    .uri(builder -> builder.path("/api/v1/learning/gaps")
                            .queryParam("systemCode", systemCode).build())
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<MasteryView>>() {
                    });
            return result == null ? List.of() : result;

        } catch (RuntimeException e) {
            log.warn("learning-service indisponible, reponse moins ciblee : {}", e.getMessage());
            return List.of();
        }
    }
}
