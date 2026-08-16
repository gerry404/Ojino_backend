package com.schoolcopilot.assistant_service.client;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.schoolcopilot.assistant_service.exception.ApiException;

/**
 * Le profil scolaire, detenu par {@code user-service}.
 *
 * <p>C'est la seule source dont la panne est <strong>bloquante</strong>. Sans le
 * niveau, on ne connait pas le cycle, donc pas le registre de langage. Repondre
 * a un enfant de six ans avec le registre d'un lyceen est pire que ne pas
 * repondre du tout.
 *
 * <p>Le jeton de l'eleve est retransmis : user-service ne renvoie donc que le
 * profil de l'appelant, et ce service ne peut pas lire celui d'un autre.
 */
@Component
public class ProfileClient {

    private static final Logger log = LoggerFactory.getLogger(ProfileClient.class);

    private final RestClient restClient;

    public ProfileClient(RestClient profileRestClient) {
        this.restClient = profileRestClient;
    }

    /** Reflet partiel de la vue exposee par user-service. */
    public record ProfileView(
            String systemCode,
            String levelCode,
            String trackCode,
            List<String> subjectCodes,
            List<String> learningDomainCodes,
            List<String> courseUnitCodes,
            boolean onboardingComplete) {

        /** Ce que l'eleve etudie, quel que soit le nom que son cycle lui donne. */
        public List<String> studied() {
            List<String> all = new java.util.ArrayList<>();
            addAll(all, subjectCodes);
            addAll(all, learningDomainCodes);
            addAll(all, courseUnitCodes);
            return all;
        }

        private void addAll(List<String> target, List<String> source) {
            if (source != null) {
                target.addAll(source);
            }
        }
    }

    public ProfileView me(String bearerToken) {
        try {
            return restClient.get()
                    .uri("/api/v1/profile/me")
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve()
                    .body(ProfileView.class);

        } catch (RuntimeException e) {
            log.error("user-service injoignable, question refusee : {}", e.getMessage());
            throw ApiException.profileUnavailable();
        }
    }
}
