package com.schoolcopilot.engagement_service.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.engagement_service.domain.Badge;
import com.schoolcopilot.engagement_service.exception.ApiException;
import com.schoolcopilot.engagement_service.service.EngagementService;
import com.schoolcopilot.engagement_service.service.StreakPolicy;
import com.schoolcopilot.engagement_service.web.dto.ActivityRequest;

import jakarta.validation.Valid;

/**
 * Le point d'entree des autres services.
 *
 * <p>{@code learning-service} signale une seance terminee ou une notion acquise,
 * {@code planning-service} une seance faite. Ce service n'observe rien de
 * lui-meme : il ne saurait pas quoi observer.
 */
@RestController
@RequestMapping("/api/v1/internal/engagement")
public class InternalEngagementController {

    private final EngagementService engagement;

    public InternalEngagementController(EngagementService engagement) {
        this.engagement = engagement;
    }

    /**
     * @param outcome dit ce qui est arrive a la serie, pour que l'appelant puisse
     *        l'annoncer — notamment quand un joker l'a sauvee
     */
    public record ActivityResponse(int currentStreak, int longestStreak,
            StreakPolicy.Outcome outcome, int freezesConsumed, int freezesAvailable,
            List<Badge> newBadges) {
    }

    @PostMapping("/activity")
    public ActivityResponse recordActivity(@Valid @RequestBody ActivityRequest request) {
        LocalDate day = request.day() == null ? LocalDate.now() : request.day();

        // Une activite future fausserait la serie de facon irrattrapable : elle
        // rendrait tous les jours suivants "deja comptes".
        if (day.isAfter(LocalDate.now())) {
            throw ApiException.futureDate();
        }

        EngagementService.ActivityResult result =
                engagement.recordActivity(request.userId(), day, request.increments());

        return new ActivityResponse(result.streak().current(), result.streak().longest(),
                result.outcome(), result.freezesConsumed(),
                result.streak().freezesAvailable(), result.newBadges());
    }
}
