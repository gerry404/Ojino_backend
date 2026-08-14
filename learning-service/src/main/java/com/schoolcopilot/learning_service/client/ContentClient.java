package com.schoolcopilot.learning_service.client;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.schoolcopilot.learning_service.exception.ApiException;

/**
 * Lecture du programme, detenu par {@code content-service}.
 *
 * <p>Ce service ne stocke aucun contenu : il ne connait que des <em>codes</em> de
 * notions et ce que l'eleve en a fait. Pour savoir ce qu'il faut reprendre quand
 * quelqu'un bloque, il interroge le graphe de prerequis la ou il vit.
 */
@Component
public class ContentClient {

    private static final Logger log = LoggerFactory.getLogger(ContentClient.class);

    private final RestClient restClient;

    public ContentClient(RestClient contentRestClient) {
        this.restClient = contentRestClient;
    }

    /** Reflet de la vue exposee par content-service. */
    public record NotionView(
            String code,
            String label,
            String summary,
            String chapterCode,
            int rank,
            List<String> prerequisiteCodes) {
    }

    /**
     * Tout ce qu'il faut maitriser avant cette notion, dans l'ordre ou le
     * reprendre. Le calcul du graphe appartient a content-service ; ici on ne fait
     * que le croiser avec ce que l'eleve maitrise deja.
     */
    public List<NotionView> learningPath(String systemCode, String notionCode) {
        return call(() -> restClient.get()
                .uri("/api/v1/curriculum/systems/{system}/notions/{notion}/learning-path",
                        systemCode, notionCode)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw translate(response.getStatusCode(), notionCode);
                })
                .body(new ParameterizedTypeReference<List<NotionView>>() {
                }));
    }

    public NotionView notion(String systemCode, String notionCode) {
        return call(() -> restClient.get()
                .uri("/api/v1/curriculum/systems/{system}/notions/{notion}", systemCode,
                        notionCode)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw translate(response.getStatusCode(), notionCode);
                })
                .body(NotionView.class));
    }

    // ------------------------------------------------------------------

    private <T> T call(java.util.function.Supplier<T> request) {
        try {
            return request.get();
        } catch (ResourceAccessException e) {
            log.error("content-service injoignable : {}", e.getMessage());
            throw ApiException.contentUnavailable();
        }
    }

    private ApiException translate(HttpStatusCode status, String notionCode) {
        if (status.value() == HttpStatus.NOT_FOUND.value()) {
            return ApiException.unknownNotion(notionCode);
        }
        log.error("content-service a repondu {} pour la notion {}", status, notionCode);
        return ApiException.contentUnavailable();
    }
}
