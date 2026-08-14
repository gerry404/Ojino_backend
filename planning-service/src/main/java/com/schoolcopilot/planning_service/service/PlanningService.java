package com.schoolcopilot.planning_service.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.schoolcopilot.planning_service.client.LearningClient;
import com.schoolcopilot.planning_service.client.ProfileClient;
import com.schoolcopilot.planning_service.domain.Deadline;
import com.schoolcopilot.planning_service.domain.SessionReason;
import com.schoolcopilot.planning_service.domain.SessionStatus;
import com.schoolcopilot.planning_service.domain.StudySession;
import com.schoolcopilot.planning_service.exception.ApiException;
import com.schoolcopilot.planning_service.repository.PlanningRepositories;

/**
 * Quand l'eleve travaille, et sur quoi.
 *
 * <p>Le service assemble ce que les autres detiennent : les disponibilites
 * viennent de {@code user-service}, les priorites de {@code learning-service} et
 * des echeances saisies ici. Rien de tout cela n'est duplique.
 */
@Service
public class PlanningService {

    private static final Logger log = LoggerFactory.getLogger(PlanningService.class);

    private final PlanningRepositories.Deadlines deadlines;
    private final PlanningRepositories.Sessions sessions;
    private final ProfileClient profiles;
    private final LearningClient learning;
    private final PriorityResolver priorities;
    private final WeeklyPlanner planner;

    public PlanningService(PlanningRepositories.Deadlines deadlines,
            PlanningRepositories.Sessions sessions, ProfileClient profiles,
            LearningClient learning, PriorityResolver priorities, WeeklyPlanner planner) {
        this.deadlines = deadlines;
        this.sessions = sessions;
        this.profiles = profiles;
        this.learning = learning;
        this.priorities = priorities;
        this.planner = planner;
    }

    // ------------------------------------------------------------------
    // Echeances
    // ------------------------------------------------------------------

    public List<Deadline> deadlinesOf(String userId) {
        return deadlines.findByUserIdOrderByDueOnAsc(userId);
    }

    public List<Deadline> upcomingDeadlines(String userId, LocalDate today) {
        return deadlines
                .findByUserIdAndCompletedFalseAndDueOnGreaterThanEqualOrderByDueOnAsc(userId, today);
    }

    public Deadline addDeadline(String userId, Deadline draft) {
        if (draft.dueOn().isBefore(LocalDate.now())) {
            throw ApiException.dueDateInPast();
        }

        return deadlines.save(new Deadline(null, userId, draft.systemCode(), draft.type(),
                draft.label(), draft.anchorCode(), draft.notionCodes(), draft.dueOn(),
                clampImportance(draft.importance()), false, Instant.now()));
    }

    public Deadline completeDeadline(String userId, String deadlineId) {
        Deadline deadline = requireDeadline(userId, deadlineId);
        return deadlines.save(new Deadline(deadline.id(), deadline.userId(),
                deadline.systemCode(), deadline.type(), deadline.label(), deadline.anchorCode(),
                deadline.notionCodes(), deadline.dueOn(), deadline.importance(), true,
                deadline.createdAt()));
    }

    public void deleteDeadline(String userId, String deadlineId) {
        deadlines.delete(requireDeadline(userId, deadlineId));
    }

    // ------------------------------------------------------------------
    // Planning
    // ------------------------------------------------------------------

    /**
     * Genere le planning d'une semaine et remplace celui qui existait.
     *
     * <p>Seules les seances encore a faire sont remplacees : ce qui a ete
     * commence, termine ou annule reste tel quel, sinon on effacerait le travail
     * deja accompli.
     *
     * @param bearerToken retransmis aux services consultes, pour qu'ils repondent
     *        au nom de l'eleve et non du service
     */
    public List<StudySession> generateWeek(String userId, LocalDate weekStart,
            String bearerToken) {

        ProfileClient.ProfileView profile = profiles.me(bearerToken);
        if (profile.availability().isEmpty()) {
            throw ApiException.noAvailability();
        }

        LocalDate monday = weekStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        String systemCode = profile.systemCode();

        List<StudyPriority> resolved = priorities.resolve(
                LocalDate.now(),
                upcomingDeadlines(userId, LocalDate.now()),
                learning.gaps(systemCode, bearerToken),
                learning.mastery(systemCode, bearerToken));

        if (resolved.isEmpty()) {
            log.info("Aucune priorite pour {} : planning vide.", userId);
            return List.of();
        }

        clearPlannedSessions(userId, monday);

        List<StudySession> generated = planner
                .plan(monday, profile.availability(), resolved).stream()
                .map(planned -> toSession(userId, systemCode, planned))
                .toList();

        log.info("{} seances generees pour {} a partir du {}.", generated.size(), userId, monday);
        return sessions.saveAll(generated);
    }

    public List<StudySession> weekOf(String userId, LocalDate weekStart) {
        LocalDate monday = weekStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return sessions.findByUserIdAndScheduledOnBetweenOrderByScheduledOnAscStartTimeAsc(
                userId, monday, monday.plusDays(6));
    }

    public List<StudySession> today(String userId) {
        LocalDate today = LocalDate.now();
        return sessions.findByUserIdAndScheduledOnBetweenOrderByScheduledOnAscStartTimeAsc(
                userId, today, today);
    }

    // ------------------------------------------------------------------
    // Suivi d'une seance
    // ------------------------------------------------------------------

    public StudySession start(String userId, String sessionId) {
        StudySession session = requireSession(userId, sessionId);
        if (session.status() != SessionStatus.PLANNED) {
            throw ApiException.invalidTransition(session.status().name(), "IN_PROGRESS");
        }
        return sessions.save(with(session, SessionStatus.IN_PROGRESS, Instant.now(), null, null));
    }

    /**
     * @param actualMinutes duree reellement passee. Facultative : sans elle on
     *        retient la duree prevue, ce qui vaut mieux que rien pour comparer.
     */
    public StudySession complete(String userId, String sessionId, Integer actualMinutes) {
        StudySession session = requireSession(userId, sessionId);
        if (!session.isOpen()) {
            throw ApiException.invalidTransition(session.status().name(), "DONE");
        }
        int minutes = actualMinutes == null ? session.plannedMinutes() : actualMinutes;
        return sessions.save(with(session, SessionStatus.DONE, session.startedAt(), Instant.now(),
                minutes));
    }

    public StudySession cancel(String userId, String sessionId) {
        StudySession session = requireSession(userId, sessionId);
        if (session.status() == SessionStatus.DONE) {
            throw ApiException.invalidTransition("DONE", "CANCELLED");
        }
        return sessions.save(with(session, SessionStatus.CANCELLED, session.startedAt(), null,
                null));
    }

    // ------------------------------------------------------------------
    // Replanification
    // ------------------------------------------------------------------

    /**
     * Recupere les seances passees non faites et les repose sur les creneaux
     * libres a venir.
     *
     * <p>C'est le vrai sujet du planificateur. Un planning qui ne se reajuste pas
     * accumule les seances en retard et devient un reproche permanent : au bout de
     * deux semaines, l'eleve ne l'ouvre plus. Les seances manquees sont donc
     * marquees comme telles — elles ne disparaissent pas du bilan — puis
     * reproposees a une date tenable.
     *
     * <p>Ne recree que ce qui tient dans les creneaux restants. Reporter dix
     * seances sur une semaine deja pleine reproduirait exactement le probleme.
     */
    public List<StudySession> replan(String userId, String bearerToken) {
        LocalDate today = LocalDate.now();

        List<StudySession> missed = sessions.findByUserIdAndStatusAndScheduledOnLessThan(
                userId, SessionStatus.PLANNED, today);

        if (missed.isEmpty()) {
            return List.of();
        }

        // Marquer d'abord : meme si aucun creneau ne se libere, ces seances ne
        // doivent plus apparaitre comme prevues a une date depassee.
        sessions.saveAll(missed.stream()
                .map(session -> with(session, SessionStatus.MISSED, null, null, null))
                .toList());

        ProfileClient.ProfileView profile = profiles.me(bearerToken);
        if (profile.availability().isEmpty()) {
            return List.of();
        }

        List<StudySession> upcoming = sessions
                .findByUserIdAndScheduledOnGreaterThanEqualAndStatus(userId, today,
                        SessionStatus.PLANNED);

        List<WeeklyPlanner.TimeSlot> free = planner
                .plan(today, profile.availability(),
                        List.of(StudyPriority.of("_", SessionReason.CATCH_UP, 1)))
                .stream()
                .map(WeeklyPlanner.PlannedSession::slot)
                .filter(slot -> upcoming.stream().noneMatch(session -> occupies(session, slot)))
                .toList();

        List<StudySession> rescheduled = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(missed.size(), free.size()); i++) {
            StudySession origin = missed.get(i);
            WeeklyPlanner.TimeSlot slot = free.get(i);

            rescheduled.add(new StudySession(null, userId, origin.systemCode(),
                    origin.notionCode(), origin.deadlineId(), SessionReason.CATCH_UP,
                    slot.date(), slot.startTime(), slot.endTime(), SessionStatus.PLANNED,
                    null, null, null, Instant.now()));
        }

        log.info("{} seances manquees pour {}, {} reproposees.", missed.size(), userId,
                rescheduled.size());
        return sessions.saveAll(rescheduled);
    }

    /** Prevu contre reel, pour que l'eleve voie ou il en est sans se juger. */
    public WeeklyReport reportOf(String userId, LocalDate weekStart) {
        List<StudySession> week = weekOf(userId, weekStart);

        int planned = week.stream().mapToInt(StudySession::plannedMinutes).sum();
        int done = week.stream()
                .filter(session -> session.status() == SessionStatus.DONE)
                .mapToInt(session -> session.actualMinutes() == null
                        ? session.plannedMinutes()
                        : session.actualMinutes())
                .sum();

        long completed = week.stream().filter(s -> s.status() == SessionStatus.DONE).count();
        long missed = week.stream().filter(s -> s.status() == SessionStatus.MISSED).count();

        return new WeeklyReport(week.size(), (int) completed, (int) missed, planned, done);
    }

    /** @param completionRate de 0 a 1, sur les minutes et non sur le nombre de seances */
    public record WeeklyReport(int totalSessions, int completedSessions, int missedSessions,
            int plannedMinutes, int doneMinutes) {

        public double completionRate() {
            return plannedMinutes == 0 ? 0 : (double) doneMinutes / plannedMinutes;
        }
    }

    // ------------------------------------------------------------------

    private void clearPlannedSessions(String userId, LocalDate monday) {
        List<StudySession> existing = weekOf(userId, monday).stream()
                .filter(session -> session.status() == SessionStatus.PLANNED)
                .toList();
        sessions.deleteAll(existing);
    }

    private boolean occupies(StudySession session, WeeklyPlanner.TimeSlot slot) {
        return session.scheduledOn().equals(slot.date())
                && session.startTime().isBefore(slot.endTime())
                && slot.startTime().isBefore(session.endTime());
    }

    private StudySession toSession(String userId, String systemCode,
            WeeklyPlanner.PlannedSession planned) {
        return new StudySession(null, userId, systemCode, planned.priority().notionCode(),
                planned.priority().deadlineId(), planned.priority().reason(),
                planned.slot().date(), planned.slot().startTime(), planned.slot().endTime(),
                SessionStatus.PLANNED, null, null, null, Instant.now());
    }

    private StudySession with(StudySession session, SessionStatus status, Instant startedAt,
            Instant completedAt, Integer actualMinutes) {
        return new StudySession(session.id(), session.userId(), session.systemCode(),
                session.notionCode(), session.deadlineId(), session.reason(),
                session.scheduledOn(), session.startTime(), session.endTime(), status,
                startedAt, completedAt, actualMinutes, session.createdAt());
    }

    /** Un identifiant devine ne doit pas donner acces au planning de quelqu'un d'autre. */
    private Deadline requireDeadline(String userId, String deadlineId) {
        Deadline deadline = deadlines.findById(deadlineId)
                .orElseThrow(() -> ApiException.deadlineNotFound(deadlineId));
        if (!deadline.userId().equals(userId)) {
            throw ApiException.notYours();
        }
        return deadline;
    }

    private StudySession requireSession(String userId, String sessionId) {
        StudySession session = sessions.findById(sessionId)
                .orElseThrow(() -> ApiException.sessionNotFound(sessionId));
        if (!session.userId().equals(userId)) {
            throw ApiException.notYours();
        }
        return session;
    }

    private int clampImportance(int importance) {
        return Math.max(1, Math.min(5, importance));
    }
}
