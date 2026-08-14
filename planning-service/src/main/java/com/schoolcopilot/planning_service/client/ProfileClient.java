package com.schoolcopilot.planning_service.client;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.schoolcopilot.planning_service.exception.ApiException;

/**
 * Les disponibilites de l'eleve, detenues par {@code user-service}.
 *
 * <p>Elles ont ete collectees a la derniere etape du parcours d'inscription. Ce
 * service ne les stocke pas : il les lit au moment de generer un planning, ce qui
 * arrive une fois par semaine et non a chaque requete.
 *
 * <p>L'appel est fait <strong>au nom de l'eleve</strong> : le token recu est
 * retransmis, et user-service ne renvoie donc que le profil de l'appelant. Aucun
 * identifiant ne circule, et ce service ne peut pas lire le profil d'un autre.
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
            List<SlotView> availability,
            boolean onboardingComplete) {

        public List<SlotView> availability() {
            return availability == null ? List.of() : availability;
        }
    }

    public record SlotView(DayOfWeek day, LocalTime startTime, LocalTime endTime, long minutes) {
    }

    public ProfileView me(String bearerToken) {
        try {
            return restClient.get()
                    .uri("/api/v1/profile/me")
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve()
                    .body(ProfileView.class);
        } catch (ResourceAccessException e) {
            log.error("user-service injoignable : {}", e.getMessage());
            throw ApiException.profileUnavailable();
        }
    }
}
