package com.schoolcopilot.learning_service.web;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.learning_service.domain.LearningEvent;
import com.schoolcopilot.learning_service.domain.MasteryLevel;
import com.schoolcopilot.learning_service.service.LearningService;
import com.schoolcopilot.learning_service.web.dto.MasteryView;
import com.schoolcopilot.learning_service.web.dto.RecordEventRequest;
import com.schoolcopilot.learning_service.web.dto.RemediationView;

import jakarta.validation.Valid;

/**
 * Le suivi d'apprentissage de l'eleve connecte.
 *
 * <p>Toutes les routes travaillent sur le {@code sub} du token : aucun
 * identifiant d'eleve ne circule, donc personne ne peut lire ni ecrire les
 * resultats d'un autre.
 */
@RestController
@RequestMapping("/api/v1/learning")
public class LearningController {

    private final LearningService learning;

    public LearningController(LearningService learning) {
        this.learning = learning;
    }

    /**
     * Enregistre un fait d'apprentissage et renvoie la maitrise recalculee.
     *
     * <p>Renvoyer l'etat a jour evite au client un second appel apres chaque
     * exercice — c'est l'appel le plus frequent du service.
     */
    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public MasteryView record(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody RecordEventRequest request) {
        return MasteryView.from(learning.record(jwt.getSubject(), request.toDomain()));
    }

    @GetMapping("/mastery")
    public List<MasteryView> mastery(@AuthenticationPrincipal Jwt jwt,
            @RequestParam String systemCode) {
        return learning.masteryFor(jwt.getSubject(), systemCode).stream()
                .map(MasteryView::from)
                .toList();
    }

    @GetMapping("/mastery/{notionCode}")
    public MasteryView masteryOf(@AuthenticationPrincipal Jwt jwt,
            @RequestParam String systemCode, @PathVariable String notionCode) {
        return MasteryView.from(learning.masteryOf(jwt.getSubject(), systemCode, notionCode));
    }

    /** Les notions ou l'eleve bloque. */
    @GetMapping("/gaps")
    public List<MasteryView> gaps(@AuthenticationPrincipal Jwt jwt,
            @RequestParam String systemCode) {
        return learning.gaps(jwt.getSubject(), systemCode).stream()
                .map(MasteryView::from)
                .toList();
    }

    /**
     * Ce qu'il faut reprendre pour debloquer une notion.
     *
     * <p>Croise le graphe de prerequis, detenu par {@code content-service}, avec ce
     * que l'eleve maitrise deja. Quelqu'un qui bloque sur les derivees se voit
     * proposer les limites, pas tout le chapitre depuis le debut.
     */
    @GetMapping("/remediation/{notionCode}")
    public List<RemediationView> remediation(@AuthenticationPrincipal Jwt jwt,
            @RequestParam String systemCode, @PathVariable String notionCode) {
        return learning.remediationFor(jwt.getSubject(), systemCode, notionCode).stream()
                .map(RemediationView::from)
                .toList();
    }

    /** Repartition par palier, pour un tableau de bord. */
    @GetMapping("/dashboard")
    public Map<MasteryLevel, Long> dashboard(@AuthenticationPrincipal Jwt jwt,
            @RequestParam String systemCode) {
        return learning.distribution(jwt.getSubject(), systemCode);
    }

    @GetMapping("/activity")
    public List<LearningEvent> activity(@AuthenticationPrincipal Jwt jwt) {
        return learning.recentActivity(jwt.getSubject());
    }
}
