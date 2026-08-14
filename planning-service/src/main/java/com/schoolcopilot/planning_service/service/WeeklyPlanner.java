package com.schoolcopilot.planning_service.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.schoolcopilot.planning_service.client.ProfileClient;
import com.schoolcopilot.planning_service.config.PlanningProperties;

/**
 * La generation du planning hebdomadaire.
 *
 * <p>Isole du service : c'est un algorithme, il se lit et se verifie sans base ni
 * appel reseau. Il ne connait ni Mongo ni HTTP — on lui donne des creneaux et des
 * priorites, il rend des seances.
 *
 * <p>Le principe tient en trois temps : decouper les disponibilites en seances,
 * classer les priorites, puis les apparier. Le plus urgent tombe le plus tot dans
 * la semaine, parce qu'une seance de fin de semaine a bien plus de chances d'etre
 * manquee qu'une seance de lundi.
 */
@Component
public class WeeklyPlanner {

    private final PlanningProperties properties;

    public WeeklyPlanner(PlanningProperties properties) {
        this.properties = properties;
    }

    /** Un creneau libre, deja decoupe a la taille d'une seance. */
    public record TimeSlot(LocalDate date, LocalTime startTime, LocalTime endTime) {

        public int minutes() {
            return (int) Duration.between(startTime, endTime).toMinutes();
        }
    }

    /** Une seance proposee : une priorite posee sur un creneau. */
    public record PlannedSession(TimeSlot slot, StudyPriority priority) {
    }

    /**
     * Construit le planning d'une semaine.
     *
     * <p>S'il y a plus de priorites que de creneaux, les moins urgentes attendent
     * la semaine suivante. S'il y a plus de creneaux que de priorites, les
     * priorites sont reprises en boucle : mieux vaut retravailler une notion deux
     * fois que laisser un creneau vide.
     *
     * @param weekStart premier jour de la semaine a planifier
     * @param availability creneaux hebdomadaires declares par l'eleve
     * @param priorities ce qu'il faut travailler, dans n'importe quel ordre
     */
    public List<PlannedSession> plan(LocalDate weekStart,
            List<ProfileClient.SlotView> availability, List<StudyPriority> priorities) {

        if (availability.isEmpty() || priorities.isEmpty()) {
            return List.of();
        }

        List<TimeSlot> slots = expand(weekStart, availability);
        List<StudyPriority> ordered = priorities.stream()
                .sorted(Comparator.comparingDouble(StudyPriority::weight).reversed())
                .toList();

        List<PlannedSession> sessions = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            // Le modulo reprend les priorites depuis le debut quand il reste des
            // creneaux : les plus urgentes reviennent donc naturellement le plus
            // souvent dans la semaine.
            sessions.add(new PlannedSession(slots.get(i), ordered.get(i % ordered.size())));
        }
        return sessions;
    }

    /**
     * Transforme des creneaux hebdomadaires recurrents en creneaux dates, decoupes
     * a la taille d'une seance.
     *
     * <p>Le resultat est trie chronologiquement : c'est ce qui garantit que la
     * priorite la plus forte tombe au creneau le plus proche.
     */
    private List<TimeSlot> expand(LocalDate weekStart,
            List<ProfileClient.SlotView> availability) {

        List<TimeSlot> slots = new ArrayList<>();

        for (int offset = 0; offset < 7; offset++) {
            LocalDate date = weekStart.plusDays(offset);
            int onThisDay = 0;

            List<ProfileClient.SlotView> ofDay = availability.stream()
                    .filter(slot -> slot.day() == date.getDayOfWeek())
                    .sorted(Comparator.comparing(ProfileClient.SlotView::startTime))
                    .toList();

            for (ProfileClient.SlotView slot : ofDay) {
                LocalTime cursor = slot.startTime();

                while (onThisDay < properties.maxSessionsPerDay()) {
                    long remaining = Duration.between(cursor, slot.endTime()).toMinutes();
                    if (remaining < properties.minSessionMinutes()) {
                        break;
                    }

                    // Un creneau plus court que la seance visee produit quand meme
                    // une seance, tant qu'elle reste utile.
                    int length = (int) Math.min(properties.sessionMinutes(), remaining);
                    LocalTime end = cursor.plusMinutes(length);

                    slots.add(new TimeSlot(date, cursor, end));
                    onThisDay++;

                    cursor = end.plusMinutes(properties.breakMinutes());
                }
            }
        }

        return slots;
    }
}
