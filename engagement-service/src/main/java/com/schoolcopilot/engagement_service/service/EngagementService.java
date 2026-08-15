package com.schoolcopilot.engagement_service.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.schoolcopilot.engagement_service.client.NotificationClient;
import com.schoolcopilot.engagement_service.config.EngagementProperties;
import com.schoolcopilot.engagement_service.domain.Badge;
import com.schoolcopilot.engagement_service.domain.EngagementProfile;
import com.schoolcopilot.engagement_service.domain.MoodCheckIn;
import com.schoolcopilot.engagement_service.domain.Streak;
import com.schoolcopilot.engagement_service.exception.ApiException;
import com.schoolcopilot.engagement_service.repository.EngagementRepositories;

/**
 * La motivation et le bien-etre.
 *
 * <p>Le service ne mesure rien lui-meme : ce sont les autres qui lui signalent
 * qu'une seance a ete faite ou qu'une notion est acquise. Il en tire une serie,
 * des badges, et surtout des signaux quand quelque chose ne va pas.
 */
@Service
public class EngagementService {

    private static final Logger log = LoggerFactory.getLogger(EngagementService.class);

    private final EngagementRepositories.Streaks streaks;
    private final EngagementRepositories.Profiles profiles;
    private final EngagementRepositories.CheckIns checkIns;
    private final StreakPolicy streakPolicy;
    private final WellbeingAnalyzer wellbeing;
    private final NotificationClient notifications;
    private final EngagementProperties properties;

    public EngagementService(EngagementRepositories.Streaks streaks,
            EngagementRepositories.Profiles profiles, EngagementRepositories.CheckIns checkIns,
            StreakPolicy streakPolicy, WellbeingAnalyzer wellbeing,
            NotificationClient notifications, EngagementProperties properties) {
        this.streaks = streaks;
        this.profiles = profiles;
        this.checkIns = checkIns;
        this.streakPolicy = streakPolicy;
        this.wellbeing = wellbeing;
        this.notifications = notifications;
        this.properties = properties;
    }

    /** Ce qui a change apres une activite, pour pouvoir l'annoncer a l'eleve. */
    public record ActivityResult(Streak streak, StreakPolicy.Outcome outcome,
            int freezesConsumed, List<Badge> newBadges) {
    }

    // ------------------------------------------------------------------
    // Activite
    // ------------------------------------------------------------------

    /**
     * Enregistre une journee d'activite.
     *
     * <p>Appele par les autres services quand une seance se termine ou qu'une
     * notion est acquise. Idempotent sur la journee : plusieurs appels le meme jour
     * ne font pas avancer la serie deux fois.
     */
    public ActivityResult recordActivity(String userId, LocalDate day,
            Map<Badge.Metric, Integer> increments) {

        Streak existing = streaks.findById(userId)
                .orElseGet(() -> Streak.start(userId, properties.initialFreezes()));

        StreakPolicy.Result result = streakPolicy.recordActivity(existing, day);
        Streak saved = streaks.save(result.streak());

        EngagementProfile profile = bumpCounters(userId, increments, saved.current(),
                result.outcome());
        List<Badge> earned = grantBadges(profile);

        if (result.outcome() == StreakPolicy.Outcome.SAVED_BY_FREEZE) {
            log.debug("Serie de {} sauvee par {} joker(s).", userId, result.freezesConsumed());
        }

        return new ActivityResult(saved, result.outcome(), result.freezesConsumed(), earned);
    }

    public Streak streakOf(String userId) {
        return streaks.findById(userId)
                .orElseGet(() -> Streak.start(userId, properties.initialFreezes()));
    }

    public EngagementProfile profileOf(String userId) {
        return profiles.findById(userId).orElseGet(() -> EngagementProfile.empty(userId));
    }

    // ------------------------------------------------------------------
    // Humeur et bien-etre
    // ------------------------------------------------------------------

    /**
     * Enregistre le releve du jour.
     *
     * <p>Un seul par jour, le dernier fait foi : quelqu'un dont la journee s'ameliore
     * doit pouvoir corriger ce qu'il a dit le matin.
     */
    public MoodCheckIn checkIn(String userId, LocalDate day, int mood, int workload,
            String note) {

        MoodCheckIn checkIn = new MoodCheckIn(MoodCheckIn.idFor(userId, day), userId, day, mood,
                workload, note, Instant.now());

        if (!checkIn.isValid()) {
            throw ApiException.invalidCheckIn();
        }
        return checkIns.save(checkIn);
    }

    /**
     * Ce que disent les derniers releves.
     *
     * <p>Le resultat n'est pas un diagnostic : c'est un signal, qui justifie tout
     * au plus de proposer une pause ou d'alleger le planning.
     */
    public WellbeingAnalyzer.Assessment wellbeingOf(String userId) {
        List<MoodCheckIn> recent = checkIns.findByUserIdOrderByDayDesc(userId);
        return wellbeing.analyze(recent, LocalDate.now());
    }

    public List<MoodCheckIn> checkInHistory(String userId, int days) {
        return checkIns.findByUserIdAndDayGreaterThanEqualOrderByDayDesc(userId,
                LocalDate.now().minusDays(days));
    }

    // ------------------------------------------------------------------
    // Relances
    // ------------------------------------------------------------------

    /**
     * Previent ceux dont la serie va se rompre aujourd'hui.
     *
     * <p>Prevenir que la serie <em>est</em> cassee ne sert a rien ; prevenir qu'elle
     * <em>va</em> l'etre laisse le choix. La cle de deduplication porte la date :
     * une tache rejouee ne produit pas deux relances.
     */
    public int notifyStreaksAtRisk() {
        LocalDate today = LocalDate.now();

        List<Streak> candidates =
                streaks.findByCurrentGreaterThanAndLastActiveOnBefore(0, today);

        List<Streak> atRisk = candidates.stream()
                .filter(streak -> streakPolicy.isAtRisk(streak, today))
                .toList();

        atRisk.forEach(streak -> notifications.notify(streak.userId(), "STREAK_AT_RISK",
                Map.of("days", String.valueOf(streak.current())),
                "streak-at-risk:" + streak.userId() + ":" + today));

        return atRisk.size();
    }

    // ------------------------------------------------------------------

    private EngagementProfile bumpCounters(String userId, Map<Badge.Metric, Integer> increments,
            int currentStreak, StreakPolicy.Outcome outcome) {

        EngagementProfile existing = profileOf(userId);
        Map<Badge.Metric, Integer> counters = new EnumMap<>(Badge.Metric.class);
        counters.putAll(existing.counters());

        if (increments != null) {
            increments.forEach((metric, delta) ->
                    counters.merge(metric, delta, Integer::sum));
        }

        // La serie n'est pas un cumul : c'est un etat. L'additionner ferait gagner
        // le badge des trente jours au bout de trente seances quelconques.
        counters.put(Badge.Metric.STREAK, Math.max(currentStreak,
                counters.getOrDefault(Badge.Metric.STREAK, 0)));

        if (outcome == StreakPolicy.Outcome.RESET || outcome == StreakPolicy.Outcome.STARTED) {
            counters.merge(Badge.Metric.COMEBACKS, 1, Integer::sum);
        }

        return profiles.save(new EngagementProfile(userId, counters, existing.badges(),
                Instant.now()));
    }

    /** Un badge obtenu ne se reprend jamais, meme si le compteur qui l'a donne recule. */
    private List<Badge> grantBadges(EngagementProfile profile) {
        List<Badge> earned = new ArrayList<>();
        Set<Badge> all = EnumSet.copyOf(profile.badges().isEmpty()
                ? EnumSet.noneOf(Badge.class)
                : EnumSet.copyOf(profile.badges()));

        for (Badge badge : Badge.values()) {
            if (!all.contains(badge) && badge.isEarnedAt(profile.counter(badge.metric()))) {
                all.add(badge);
                earned.add(badge);
            }
        }

        if (!earned.isEmpty()) {
            profiles.save(new EngagementProfile(profile.userId(), profile.counters(), all,
                    Instant.now()));
        }
        return earned;
    }
}
