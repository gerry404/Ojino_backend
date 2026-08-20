package com.schoolcopilot.assistant_service.client;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Le programme, detenu par {@code content-service}.
 *
 * <p>Panne <strong>non bloquante</strong>, mais signalee : la reponse ne sera pas
 * ancree dans le programme, et la tracabilite doit permettre de comprendre
 * pourquoi une reponse d'un jour donne etait plus vague que d'habitude.
 */
@Component
public class ContentClient {

    private static final Logger log = LoggerFactory.getLogger(ContentClient.class);

    private final RestClient restClient;

    public ContentClient(RestClient contentRestClient) {
        this.restClient = contentRestClient;
    }

    public record NotionView(
            String code,
            String label,
            String summary,
            String chapterCode,
            List<String> prerequisiteCodes) {

        public List<String> prerequisiteCodes() {
            return prerequisiteCodes == null ? List.of() : prerequisiteCodes;
        }
    }

    public record LevelView(String code, String label, String cycle) {
    }

    /** Le cycle du niveau : c'est de lui que derive le registre de langage. */
    public Optional<LevelView> level(String systemCode, String levelCode) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/api/v1/reference/systems/{system}/levels/{level}",
                            systemCode, levelCode)
                    .retrieve()
                    .body(LevelView.class));

        } catch (RuntimeException e) {
            log.warn("content-service indisponible pour le niveau {} : {}", levelCode,
                    e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<NotionView> notion(String systemCode, String notionCode) {
        try {
            return Optional.ofNullable(restClient.get()
                    .uri("/api/v1/curriculum/systems/{system}/notions/{notion}",
                            systemCode, notionCode)
                    .retrieve()
                    .body(NotionView.class));

        } catch (RuntimeException e) {
            log.warn("content-service indisponible pour la notion {} : {}", notionCode,
                    e.getMessage());
            return Optional.empty();
        }
    }

    /** Les notions a reprendre avant celle-ci. */
    public List<NotionView> learningPath(String systemCode, String notionCode) {
        try {
            List<NotionView> result = restClient.get()
                    .uri("/api/v1/curriculum/systems/{system}/notions/{notion}/learning-path",
                            systemCode, notionCode)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<NotionView>>() {
                    });
            return result == null ? List.of() : result;

        } catch (RuntimeException e) {
            log.warn("parcours de rattrapage indisponible pour {} : {}", notionCode,
                    e.getMessage());
            return List.of();
        }
    }
}
