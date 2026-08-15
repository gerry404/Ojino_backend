package com.schoolcopilot.engagement_service.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.engagement_service.domain.Badge;
import com.schoolcopilot.engagement_service.domain.EngagementProfile;
import com.schoolcopilot.engagement_service.domain.MoodCheckIn;
import com.schoolcopilot.engagement_service.service.EngagementService;
import com.schoolcopilot.engagement_service.service.WellbeingAnalyzer;
import com.schoolcopilot.engagement_service.web.dto.CheckInRequest;
import com.schoolcopilot.engagement_service.web.dto.StreakView;

import jakarta.validation.Valid;

/**
 * La motivation de l'utilisateur connecte.
 *
 * <p>Toutes les routes travaillent sur le {@code sub} du token. Les releves
 * d'humeur en particulier ne doivent etre lisibles que par leur auteur.
 */
@RestController
@RequestMapping("/api/v1/engagement")
public class EngagementController {

    private final EngagementService engagement;

    public EngagementController(EngagementService engagement) {
        this.engagement = engagement;
    }

    @GetMapping("/streak")
    public StreakView streak(@AuthenticationPrincipal Jwt jwt) {
        return StreakView.from(engagement.streakOf(jwt.getSubject()), LocalDate.now());
    }

    @GetMapping("/profile")
    public EngagementProfile profile(@AuthenticationPrincipal Jwt jwt) {
        return engagement.profileOf(jwt.getSubject());
    }

    /** Le catalogue complet, pour montrer ce qui reste a obtenir. */
    @GetMapping("/badges")
    public List<BadgeView> badges(@AuthenticationPrincipal Jwt jwt) {
        EngagementProfile profile = engagement.profileOf(jwt.getSubject());

        return java.util.Arrays.stream(Badge.values())
                .map(badge -> new BadgeView(badge, badge.label(), badge.threshold(),
                        profile.counter(badge.metric()), profile.hasBadge(badge)))
                .toList();
    }

    /** @param progress avancement vers le seuil, pour ne pas afficher un badge opaque */
    public record BadgeView(Badge badge, String label, int threshold, int progress,
            boolean earned) {
    }

    // ------------------------------------------------------------------
    // Humeur
    // ------------------------------------------------------------------

    /** Un releve par jour ; le dernier remplace le precedent. */
    @PostMapping("/check-in")
    public MoodCheckIn checkIn(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CheckInRequest request) {
        return engagement.checkIn(jwt.getSubject(), LocalDate.now(), request.mood(),
                request.workload(), request.note());
    }

    @GetMapping("/check-ins")
    public List<MoodCheckIn> checkIns(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "30") int days) {
        return engagement.checkInHistory(jwt.getSubject(), days);
    }

    /**
     * Ce que disent les derniers releves.
     *
     * <p>Ce n'est pas un diagnostic : c'est un signal, qui justifie tout au plus de
     * proposer une pause ou d'alleger le planning.
     */
    @GetMapping("/wellbeing")
    public WellbeingAnalyzer.Assessment wellbeing(@AuthenticationPrincipal Jwt jwt) {
        return engagement.wellbeingOf(jwt.getSubject());
    }
}
