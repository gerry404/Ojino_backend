package com.schoolcopilot.content.core.web.client;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.content.core.domain.EducationCycle;
import com.schoolcopilot.content.core.domain.EducationLevel;
import com.schoolcopilot.content.core.service.ReferenceService;
import com.schoolcopilot.content.core.web.client.dto.CycleView;
import com.schoolcopilot.content.core.web.client.dto.EducationLevelView;
import com.schoolcopilot.content.core.web.client.dto.EducationSystemView;
import com.schoolcopilot.content.core.web.client.dto.SubjectView;
import com.schoolcopilot.content.core.web.client.dto.TrackView;

/**
 * Le referentiel scolaire commun a tous les cycles.
 *
 * <p>Ces routes sont publiques : les ecrans d'inscription en ont besoin avant
 * meme qu'un profil existe, et elles ne revelent aucune donnee personnelle.
 *
 * <p>Elles ont deux publics. Les applications les appellent pour remplir leurs
 * listes de choix ; {@code user-service} les appelle pour valider ce que l'eleve a
 * choisi. Les elements archives n'y apparaissent jamais — c'est ici, et non chez
 * l'appelant, que se decide ce qui reste choisissable.
 *
 * <p>Ce qui est propre a un cycle vit dans son module et sous son prefixe, par
 * exemple {@code /api/v1/reference/earlyyears}.
 */
@RestController
@RequestMapping("/api/v1/reference")
public class ReferenceController {

    private final ReferenceService reference;

    public ReferenceController(ReferenceService reference) {
        this.reference = reference;
    }

    @GetMapping("/systems")
    public List<EducationSystemView> systems() {
        return reference.listSystems().stream().map(EducationSystemView::from).toList();
    }

    /**
     * Les cycles que ce systeme propose et que ce deploiement sait servir, avec
     * les choix que chacun demande.
     */
    @GetMapping("/systems/{systemCode}/cycles")
    public List<CycleView> cycles(@PathVariable String systemCode) {
        return reference.cyclesOf(systemCode).stream().map(CycleView::from).toList();
    }

    /**
     * Les classes du systeme, dans l'ordre.
     *
     * @param age facultatif. Fourni, il fait remonter {@code suggested: true} sur
     *        la ou les classes correspondantes — une suggestion, jamais un filtre.
     * @param cycle facultatif, pour n'afficher qu'un cycle
     */
    @GetMapping("/systems/{systemCode}/levels")
    public List<EducationLevelView> levels(
            @PathVariable String systemCode,
            @RequestParam(required = false) Integer age,
            @RequestParam(required = false) EducationCycle cycle) {

        return reference.levelsFor(systemCode, age, cycle).stream()
                .map(suggested -> EducationLevelView.from(suggested,
                        reference.stepsFor(suggested.level().cycle())))
                .toList();
    }

    /**
     * Une classe precise.
     *
     * <p>C'est la route de validation : elle repond 404 pour un niveau inconnu et
     * 409 pour un niveau archive. {@code user-service} s'en sert quand l'eleve
     * choisit sa classe, et y lit {@code steps} pour savoir quels choix enchainer
     * ensuite — filiere et matieres au lycee, domaines d'apprentissage en
     * maternelle.
     */
    @GetMapping("/systems/{systemCode}/levels/{levelCode}")
    public EducationLevelView level(@PathVariable String systemCode,
            @PathVariable String levelCode) {
        EducationLevel level = reference.requireSelectableLevel(systemCode, levelCode);
        return EducationLevelView.of(level, reference.stepsFor(level.cycle()));
    }

    /** Les filieres de ce niveau. Liste vide quand le cycle n'en demande pas. */
    @GetMapping("/systems/{systemCode}/levels/{levelCode}/tracks")
    public List<TrackView> tracks(@PathVariable String systemCode, @PathVariable String levelCode) {
        return reference.tracksFor(systemCode, levelCode).stream().map(TrackView::from).toList();
    }

    /** Les matieres. Liste vide pour un cycle qui ne raisonne pas en matieres. */
    @GetMapping("/systems/{systemCode}/subjects")
    public List<SubjectView> subjects(
            @PathVariable String systemCode,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String track) {
        return reference.subjectsFor(systemCode, level, track).stream()
                .map(SubjectView::from)
                .toList();
    }
}
