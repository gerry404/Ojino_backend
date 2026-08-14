package com.schoolcopilot.planning_service.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.planning_service.service.PlanningService;
import com.schoolcopilot.planning_service.web.dto.DeadlineRequest;
import com.schoolcopilot.planning_service.web.dto.DeadlineView;
import com.schoolcopilot.planning_service.web.dto.SessionView;

import jakarta.validation.Valid;

/**
 * Le planning de l'eleve connecte.
 *
 * <p>Toutes les routes travaillent sur le {@code sub} du token : aucun
 * identifiant d'eleve ne circule, et un identifiant de seance devine ne donne
 * acces a rien puisque l'appartenance est verifiee.
 *
 * <p>Le jeton est retransmis aux services consultes, pour qu'ils repondent au nom
 * de l'eleve et non du service.
 */
@RestController
@RequestMapping("/api/v1/planning")
public class PlanningController {

    private final PlanningService planning;

    public PlanningController(PlanningService planning) {
        this.planning = planning;
    }

    // ------------------------------------------------------------------
    // Echeances
    // ------------------------------------------------------------------

    @GetMapping("/deadlines")
    public List<DeadlineView> deadlines(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "false") boolean upcomingOnly) {

        return (upcomingOnly
                ? planning.upcomingDeadlines(jwt.getSubject(), LocalDate.now())
                : planning.deadlinesOf(jwt.getSubject()))
                .stream()
                .map(DeadlineView::from)
                .toList();
    }

    @PostMapping("/deadlines")
    @ResponseStatus(HttpStatus.CREATED)
    public DeadlineView addDeadline(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody DeadlineRequest request) {
        return DeadlineView.from(planning.addDeadline(jwt.getSubject(), request.toDomain()));
    }

    @PostMapping("/deadlines/{id}/complete")
    public DeadlineView completeDeadline(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String id) {
        return DeadlineView.from(planning.completeDeadline(jwt.getSubject(), id));
    }

    @DeleteMapping("/deadlines/{id}")
    public Map<String, String> deleteDeadline(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String id) {
        planning.deleteDeadline(jwt.getSubject(), id);
        return Map.of("status", "deleted");
    }

    // ------------------------------------------------------------------
    // Planning
    // ------------------------------------------------------------------

    /**
     * Genere le planning d'une semaine.
     *
     * <p>Remplace les seances encore a faire ; ce qui a ete commence, termine ou
     * annule reste intact.
     */
    @PostMapping("/weeks/generate")
    public List<SessionView> generate(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {

        LocalDate start = weekStart == null ? LocalDate.now() : weekStart;
        return planning.generateWeek(jwt.getSubject(), start, jwt.getTokenValue()).stream()
                .map(SessionView::from)
                .toList();
    }

    @GetMapping("/weeks")
    public List<SessionView> week(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {

        LocalDate start = weekStart == null ? LocalDate.now() : weekStart;
        return planning.weekOf(jwt.getSubject(), start).stream().map(SessionView::from).toList();
    }

    @GetMapping("/today")
    public List<SessionView> today(@AuthenticationPrincipal Jwt jwt) {
        return planning.today(jwt.getSubject()).stream().map(SessionView::from).toList();
    }

    /**
     * Rattrape les seances manquees et les repose sur les creneaux libres.
     *
     * <p>A appeler a l'ouverture de l'application : un planning qui ne se reajuste
     * pas accumule les retards et finit par etre abandonne.
     */
    @PostMapping("/replan")
    public List<SessionView> replan(@AuthenticationPrincipal Jwt jwt) {
        return planning.replan(jwt.getSubject(), jwt.getTokenValue()).stream()
                .map(SessionView::from)
                .toList();
    }

    // ------------------------------------------------------------------
    // Suivi d'une seance
    // ------------------------------------------------------------------

    @PostMapping("/sessions/{id}/start")
    public SessionView start(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return SessionView.from(planning.start(jwt.getSubject(), id));
    }

    @PostMapping("/sessions/{id}/complete")
    public SessionView complete(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @RequestParam(required = false) Integer actualMinutes) {
        return SessionView.from(planning.complete(jwt.getSubject(), id, actualMinutes));
    }

    @PostMapping("/sessions/{id}/cancel")
    public SessionView cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return SessionView.from(planning.cancel(jwt.getSubject(), id));
    }

    /** Prevu contre reel, pour que l'eleve voie ou il en est. */
    @GetMapping("/weeks/report")
    public PlanningService.WeeklyReport report(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {

        LocalDate start = weekStart == null ? LocalDate.now() : weekStart;
        return planning.reportOf(jwt.getSubject(), start);
    }
}
