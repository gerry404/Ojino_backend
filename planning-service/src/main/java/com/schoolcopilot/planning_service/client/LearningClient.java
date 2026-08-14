package com.schoolcopilot.planning_service.client;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.schoolcopilot.planning_service.exception.ApiException;

/**
 * Ce que l'eleve maitrise, detenu par {@code learning-service}.
 *
 * <p>C'est la seconde moitie de ce qu'il faut pour planifier : les disponibilites
 * disent <em>quand</em>, la maitrise dit <em>quoi</em>.
 *
 * <p>Une panne ici n'empeche pas de planifier : on se rabat sur les echeances
 * seules. Un planning imparfait vaut mieux que pas de planning du tout.
 */
@Component
public class LearningClient {

    private static final Logger log = LoggerFactory.getLogger(LearningClient.class);

    private final RestClient restClient;

    public LearningClient(RestClient learningRestClient) {
        this.restClient = learningRestClient;
    }

    /** Reflet de la vue exposee par learning-service. */
    public record MasteryView(
            String notionCode,
            String level,
            double score,
            int attempts,
            Instant lastEventAt) {

        public boolean isMastered() {
            return "MASTERED".equals(level);
        }
    }

    /**
     * Les notions ou l'eleve bloque.
     *
     * <p>Renvoie une liste vide si learning-service ne repond pas, plutot que de
     * faire echouer la generation : sans elle le planning se limite aux echeances,
     * ce qui reste utile.
     */
    public List<MasteryView> gaps(String systemCode, String bearerToken) {
        return callOrEmpty(() -> restClient.get()
                .uri(builder -> builder.path("/api/v1/learning/gaps")
                        .queryParam("systemCode", systemCode).build())
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve()
                .body(new ParameterizedTypeReference<List<MasteryView>>() {
                }));
    }

    /** L'etat de maitrise complet, pour reperer ce qui merite un entretien. */
    public List<MasteryView> mastery(String systemCode, String bearerToken) {
        return callOrEmpty(() -> restClient.get()
                .uri(builder -> builder.path("/api/v1/learning/mastery")
                        .queryParam("systemCode", systemCode).build())
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve()
                .body(new ParameterizedTypeReference<List<MasteryView>>() {
                }));
    }

    private List<MasteryView> callOrEmpty(java.util.function.Supplier<List<MasteryView>> request) {
        try {
            List<MasteryView> result = request.get();
            return result == null ? List.of() : result;
        } catch (ResourceAccessException | ApiException e) {
            log.warn("learning-service indisponible, planification sur les echeances seules : {}",
                    e.getMessage());
            return List.of();
        }
    }
}
