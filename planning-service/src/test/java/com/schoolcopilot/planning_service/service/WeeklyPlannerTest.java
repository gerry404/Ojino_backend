package com.schoolcopilot.planning_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schoolcopilot.planning_service.client.ProfileClient;
import com.schoolcopilot.planning_service.config.PlanningProperties;
import com.schoolcopilot.planning_service.domain.SessionReason;

/**
 * Le planificateur est un algorithme pur : il se verifie sans base ni reseau.
 *
 * <p>Reglages des tests : seances de 45 minutes, 20 minimum, 10 de pause, 4 par
 * jour au plus.
 */
class WeeklyPlannerTest {

    private final WeeklyPlanner planner = new WeeklyPlanner(
            new PlanningProperties(45, 20, 10, 4, 14));

    /** Un lundi, pour que les decalages de jour soient lisibles. */
    private final LocalDate monday = LocalDate.of(2026, 8, 17);

    @Test
    @DisplayName("un creneau de deux heures se decoupe en seances avec des pauses")
    void aSlotIsSplitIntoSessions() {
        List<WeeklyPlanner.PlannedSession> plan = planner.plan(monday,
                List.of(slot(DayOfWeek.MONDAY, "18:00", "20:00")),
                List.of(priority("LIMITES", 3)));

        // 45 + 10 + 45 + 10 = 110 min, il reste 10 min : trop court pour une
        // troisieme seance.
        assertThat(plan).hasSize(2);
        assertThat(plan.get(0).slot().startTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(plan.get(0).slot().endTime()).isEqualTo(LocalTime.of(18, 45));
        assertThat(plan.get(1).slot().startTime()).isEqualTo(LocalTime.of(18, 55));
    }

    @Test
    @DisplayName("un reste de creneau trop court ne produit pas de seance")
    void tooShortRemainderIsLeftFree() {
        List<WeeklyPlanner.PlannedSession> plan = planner.plan(monday,
                List.of(slot(DayOfWeek.MONDAY, "18:00", "18:15")),
                List.of(priority("LIMITES", 3)));

        // Quinze minutes ne suffisent pas a une seance utile.
        assertThat(plan).isEmpty();
    }

    @Test
    @DisplayName("un creneau plus court que la seance visee produit quand meme une seance")
    void shortSlotStillProducesASession() {
        List<WeeklyPlanner.PlannedSession> plan = planner.plan(monday,
                List.of(slot(DayOfWeek.MONDAY, "18:00", "18:30")),
                List.of(priority("LIMITES", 3)));

        assertThat(plan).hasSize(1);
        assertThat(plan.get(0).slot().minutes()).isEqualTo(30);
    }

    @Test
    @DisplayName("le plafond quotidien est respecte")
    void dailyCapIsHonoured() {
        List<WeeklyPlanner.PlannedSession> plan = planner.plan(monday,
                List.of(slot(DayOfWeek.MONDAY, "08:00", "20:00")),
                List.of(priority("LIMITES", 3)));

        // Douze heures disponibles, mais un planning intenable se fait abandonner.
        assertThat(plan).hasSize(4);
    }

    @Test
    @DisplayName("la priorite la plus forte tombe au creneau le plus proche")
    void mostUrgentComesFirst() {
        List<WeeklyPlanner.PlannedSession> plan = planner.plan(monday,
                List.of(slot(DayOfWeek.MONDAY, "18:00", "18:45"),
                        slot(DayOfWeek.WEDNESDAY, "18:00", "18:45")),
                List.of(priority("FACILE", 1), priority("URGENT", 9)));

        // Une seance de fin de semaine a bien plus de chances d'etre manquee.
        assertThat(plan.get(0).priority().notionCode()).isEqualTo("URGENT");
        assertThat(plan.get(1).priority().notionCode()).isEqualTo("FACILE");
    }

    @Test
    @DisplayName("les priorites sont reprises en boucle quand il reste des creneaux")
    void prioritiesCycleWhenSlotsRemain() {
        List<WeeklyPlanner.PlannedSession> plan = planner.plan(monday,
                List.of(slot(DayOfWeek.MONDAY, "18:00", "20:00")),
                List.of(priority("SEULE", 3)));

        // Mieux vaut retravailler une notion deux fois que laisser un creneau vide.
        assertThat(plan).hasSize(2);
        assertThat(plan).allMatch(session -> session.priority().notionCode().equals("SEULE"));
    }

    @Test
    @DisplayName("les creneaux sont poses au bon jour de la semaine")
    void slotsLandOnTheRightWeekday() {
        List<WeeklyPlanner.PlannedSession> plan = planner.plan(monday,
                List.of(slot(DayOfWeek.SATURDAY, "10:00", "10:45")),
                List.of(priority("LIMITES", 3)));

        assertThat(plan).hasSize(1);
        assertThat(plan.get(0).slot().date()).isEqualTo(monday.plusDays(5));
        assertThat(plan.get(0).slot().date().getDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY);
    }

    @Test
    @DisplayName("sans disponibilite, aucun planning")
    void noAvailabilityMeansNoPlan() {
        assertThat(planner.plan(monday, List.of(), List.of(priority("LIMITES", 3)))).isEmpty();
    }

    @Test
    @DisplayName("sans priorite, aucun planning")
    void noPriorityMeansNoPlan() {
        assertThat(planner.plan(monday, List.of(slot(DayOfWeek.MONDAY, "18:00", "20:00")),
                List.of())).isEmpty();
    }

    private ProfileClient.SlotView slot(DayOfWeek day, String start, String end) {
        LocalTime from = LocalTime.parse(start);
        LocalTime to = LocalTime.parse(end);
        return new ProfileClient.SlotView(day, from, to,
                java.time.Duration.between(from, to).toMinutes());
    }

    private StudyPriority priority(String notionCode, double weight) {
        return StudyPriority.of(notionCode, SessionReason.REMEDIATION, weight);
    }
}
