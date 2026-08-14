package com.schoolcopilot.user_service.client;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.schoolcopilot.user_service.exception.ApiException;

/**
 * Lecture du referentiel scolaire, detenu par {@code content-service}.
 *
 * <p>Ce service ne stocke que les <em>codes</em> choisis par l'eleve. La question
 * de savoir si un code est valide, et s'il l'est encore, appartient a
 * {@code content-service} : c'est lui qui connait les niveaux archives, les
 * filieres disponibles par classe et les matieres pertinentes.
 *
 * <p>Les appels n'ont lieu que sur les <strong>ecritures</strong> du parcours
 * d'inscription — huit fois dans la vie d'un compte. La lecture de l'etat du
 * parcours, elle, ne declenche aucun appel reseau : le seul renseignement dont
 * elle a besoin, {@code hasTracks}, est recopie sur le profil au moment du choix.
 */
@Component
public class ContentClient {

    private static final Logger log = LoggerFactory.getLogger(ContentClient.class);

    private final RestClient restClient;

    public ContentClient(RestClient contentRestClient) {
        this.restClient = contentRestClient;
    }

    /**
     * Reflet de la vue exposee par content-service.
     *
     * @param steps les choix que le cycle impose apres la classe. C'est ce champ
     *        qui rend le parcours d'inscription independant des cycles : ce
     *        service ne sait pas ce qu'est un lycee, il suit la sequence annoncee.
     */
    public record LevelView(
            String code,
            String label,
            String cycle,
            List<String> steps,
            int rank,
            int typicalAgeMin,
            int typicalAgeMax,
            boolean hasTracks,
            boolean suggested) {

        public List<String> steps() {
            return steps == null ? List.of() : steps;
        }
    }

    public record TrackView(String code, String label, String description) {
    }

    public record SubjectView(String code, String label, boolean core) {
    }

    public record LearningDomainView(String code, String label, String description) {
    }

    public record ProgramView(String code, String label, String degree, String faculty,
            int semesterCount) {
    }

    public record CourseUnitView(String code, String label, int semester, int credits,
            boolean mandatory) {
    }

    /**
     * Le niveau, s'il existe et peut encore etre choisi.
     *
     * @throws ApiException 404 si le niveau est inconnu, 409 s'il est archive,
     *         503 si content-service ne repond pas
     */
    public LevelView requireSelectableLevel(String systemCode, String levelCode) {
        return call(() -> restClient.get()
                .uri("/api/v1/reference/systems/{system}/levels/{level}", systemCode, levelCode)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw translate(response.getStatusCode(), "Le niveau " + levelCode);
                })
                .body(LevelView.class));
    }

    public List<TrackView> tracksFor(String systemCode, String levelCode) {
        return call(() -> restClient.get()
                .uri("/api/v1/reference/systems/{system}/levels/{level}/tracks",
                        systemCode, levelCode)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw translate(response.getStatusCode(), "Le niveau " + levelCode);
                })
                .body(new ParameterizedTypeReference<List<TrackView>>() {
                }));
    }

    public List<SubjectView> subjectsFor(String systemCode, String levelCode, String trackCode) {
        return call(() -> restClient.get()
                .uri(builder -> builder
                        .path("/api/v1/reference/systems/{system}/subjects")
                        .queryParam("level", levelCode)
                        .queryParamIfPresent("track", java.util.Optional.ofNullable(trackCode))
                        .build(systemCode))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw translate(response.getStatusCode(), "Le systeme " + systemCode);
                })
                .body(new ParameterizedTypeReference<List<SubjectView>>() {
                }));
    }

    // ------------------------------------------------------------------
    // Cycles a vocabulaire propre
    // ------------------------------------------------------------------

    /** Maternelle et CP. */
    public List<LearningDomainView> learningDomainsFor(String systemCode, String levelCode) {
        return call(() -> restClient.get()
                .uri("/api/v1/reference/earlyyears/systems/{system}/levels/{level}/domains",
                        systemCode, levelCode)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw translate(response.getStatusCode(), "Le niveau " + levelCode);
                })
                .body(new ParameterizedTypeReference<List<LearningDomainView>>() {
                }));
    }

    /** Superieur. */
    public List<ProgramView> programsFor(String systemCode) {
        return call(() -> restClient.get()
                .uri("/api/v1/reference/university/systems/{system}/programs", systemCode)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw translate(response.getStatusCode(), "Le systeme " + systemCode);
                })
                .body(new ParameterizedTypeReference<List<ProgramView>>() {
                }));
    }

    public List<CourseUnitView> courseUnitsFor(String systemCode, String programCode,
            Integer semester) {
        return call(() -> restClient.get()
                .uri(builder -> builder
                        .path("/api/v1/reference/university/systems/{system}/programs/{program}/units")
                        .queryParamIfPresent("semester", java.util.Optional.ofNullable(semester))
                        .build(systemCode, programCode))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw translate(response.getStatusCode(), "Le parcours " + programCode);
                })
                .body(new ParameterizedTypeReference<List<CourseUnitView>>() {
                }));
    }

    // ------------------------------------------------------------------

    /**
     * Un content-service injoignable n'est pas une erreur de l'eleve : on renvoie
     * 503 et non 400, pour que le client sache qu'il peut reessayer.
     */
    private <T> T call(java.util.function.Supplier<T> request) {
        try {
            return request.get();
        } catch (ResourceAccessException e) {
            log.error("content-service injoignable : {}", e.getMessage());
            throw ApiException.contentUnavailable();
        }
    }

    private ApiException translate(HttpStatusCode status, String subject) {
        if (status.value() == HttpStatus.NOT_FOUND.value()) {
            return new ApiException(HttpStatus.BAD_REQUEST, "unknown_reference",
                    subject + " est inconnu du referentiel.");
        }
        if (status.value() == HttpStatus.CONFLICT.value()) {
            return new ApiException(HttpStatus.CONFLICT, "archived",
                    subject + " a ete archive et ne peut plus etre choisi.");
        }
        log.error("content-service a repondu {} pour {}", status, subject);
        return ApiException.contentUnavailable();
    }
}
