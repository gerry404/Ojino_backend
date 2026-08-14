package com.schoolcopilot.user_service.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.user_service.service.reference.ReferenceService;
import com.schoolcopilot.user_service.web.dto.EducationLevelView;
import com.schoolcopilot.user_service.web.dto.EducationSystemView;
import com.schoolcopilot.user_service.web.dto.SubjectView;
import com.schoolcopilot.user_service.web.dto.TrackView;

/**
 * Le referentiel scolaire.
 *
 * <p>Ces routes sont publiques : les ecrans d'inscription en ont besoin avant
 * meme qu'un profil existe, et elles ne revelent aucune donnee personnelle.
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
     * Les classes du systeme, dans l'ordre.
     *
     * @param age facultatif. Fourni, il fait remonter {@code suggested: true} sur
     *        la ou les classes correspondantes — une suggestion, jamais un filtre.
     */
    @GetMapping("/systems/{systemCode}/levels")
    public List<EducationLevelView> levels(
            @PathVariable String systemCode,
            @RequestParam(required = false) Integer age) {
        return reference.levelsFor(systemCode, age).stream()
                .map(EducationLevelView::from)
                .toList();
    }

    /** Les filieres de ce niveau. Liste vide quand le niveau n'en a pas. */
    @GetMapping("/systems/{systemCode}/levels/{levelCode}/tracks")
    public List<TrackView> tracks(@PathVariable String systemCode, @PathVariable String levelCode) {
        return reference.tracksFor(systemCode, levelCode).stream().map(TrackView::from).toList();
    }

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
