package com.schoolcopilot.content.core.web.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.content.core.domain.EducationLevel;
import com.schoolcopilot.content.core.domain.EducationSystem;
import com.schoolcopilot.content.core.domain.Subject;
import com.schoolcopilot.content.core.domain.Track;
import com.schoolcopilot.content.core.service.AdminReferenceService;
import com.schoolcopilot.content.core.web.admin.dto.LevelUpsertRequest;
import com.schoolcopilot.content.core.web.admin.dto.SubjectUpsertRequest;
import com.schoolcopilot.content.core.web.admin.dto.SystemUpsertRequest;
import com.schoolcopilot.content.core.web.admin.dto.TrackUpsertRequest;

import jakarta.validation.Valid;

/**
 * Pilotage du referentiel scolaire.
 *
 * <p>Les reponses renvoient les documents du referentiel tels quels : ce sont des
 * donnees de configuration, sans rien de personnel ni de secret a projeter. Une
 * couche de vue n'apporterait ici qu'un fichier de plus a maintenir.
 *
 * <p>Tout est protege par le prefixe {@code /api/v1/admin}, qui exige
 * {@code ROLE_ADMIN} au niveau de la chaine de filtres.
 */
@RestController
@RequestMapping("/api/v1/admin/reference")
public class AdminReferenceController {

    private final AdminReferenceService reference;

    public AdminReferenceController(AdminReferenceService reference) {
        this.reference = reference;
    }

    // ------------------------------------------------------------------
    // Systemes
    // ------------------------------------------------------------------

    /** Contrairement a la route publique, inclut les systemes desactives. */
    @GetMapping("/systems")
    public List<EducationSystem> systems() {
        return reference.listSystems();
    }

    @PostMapping("/systems")
    @ResponseStatus(HttpStatus.CREATED)
    public EducationSystem createSystem(@Valid @RequestBody SystemUpsertRequest request) {
        return reference.createSystem(request.toDomain());
    }

    @PutMapping("/systems/{systemCode}")
    public EducationSystem updateSystem(@PathVariable String systemCode,
            @Valid @RequestBody SystemUpsertRequest request) {
        return reference.updateSystem(systemCode, request.toDomain());
    }

    /**
     * Desactive ou reactive un systeme. Il n'existe volontairement pas de
     * suppression : les profils rattaches doivent rester lisibles.
     */
    @PostMapping("/systems/{systemCode}/active")
    public EducationSystem setSystemActive(@PathVariable String systemCode,
            @RequestParam boolean value) {
        return reference.setSystemActive(systemCode, value);
    }

    // ------------------------------------------------------------------
    // Niveaux
    // ------------------------------------------------------------------

    @GetMapping("/systems/{systemCode}/levels")
    public List<EducationLevel> levels(@PathVariable String systemCode) {
        return reference.listLevels(systemCode);
    }

    @PostMapping("/systems/{systemCode}/levels")
    @ResponseStatus(HttpStatus.CREATED)
    public EducationLevel createLevel(@PathVariable String systemCode,
            @Valid @RequestBody LevelUpsertRequest request) {
        return reference.createLevel(systemCode, request.toDomain());
    }

    @PutMapping("/systems/{systemCode}/levels/{code}")
    public EducationLevel updateLevel(@PathVariable String systemCode, @PathVariable String code,
            @Valid @RequestBody LevelUpsertRequest request) {
        return reference.updateLevel(systemCode, code, request.toDomain());
    }

    /**
     * Archive ou desarchive un niveau.
     *
     * <p>Il n'existe pas de suppression : un niveau archive disparait des choix
     * proposes mais reste resolvable, donc les profils qui le referencent
     * continuent de fonctionner.
     */
    @PostMapping("/systems/{systemCode}/levels/{code}/archived")
    public EducationLevel setLevelArchived(@PathVariable String systemCode,
            @PathVariable String code, @RequestParam boolean value) {
        return reference.setLevelArchived(systemCode, code, value);
    }

    // ------------------------------------------------------------------
    // Filieres
    // ------------------------------------------------------------------

    @GetMapping("/systems/{systemCode}/tracks")
    public List<Track> tracks(@PathVariable String systemCode) {
        return reference.listTracks(systemCode);
    }

    @PostMapping("/systems/{systemCode}/tracks")
    @ResponseStatus(HttpStatus.CREATED)
    public Track createTrack(@PathVariable String systemCode,
            @Valid @RequestBody TrackUpsertRequest request) {
        return reference.createTrack(systemCode, request.toDomain());
    }

    @PutMapping("/systems/{systemCode}/tracks/{code}")
    public Track updateTrack(@PathVariable String systemCode, @PathVariable String code,
            @Valid @RequestBody TrackUpsertRequest request) {
        return reference.updateTrack(systemCode, code, request.toDomain());
    }

    @PostMapping("/systems/{systemCode}/tracks/{code}/archived")
    public Track setTrackArchived(@PathVariable String systemCode, @PathVariable String code,
            @RequestParam boolean value) {
        return reference.setTrackArchived(systemCode, code, value);
    }

    // ------------------------------------------------------------------
    // Matieres
    // ------------------------------------------------------------------

    @GetMapping("/systems/{systemCode}/subjects")
    public List<Subject> subjects(@PathVariable String systemCode) {
        return reference.listSubjects(systemCode);
    }

    @PostMapping("/systems/{systemCode}/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    public Subject createSubject(@PathVariable String systemCode,
            @Valid @RequestBody SubjectUpsertRequest request) {
        return reference.createSubject(systemCode, request.toDomain());
    }

    @PutMapping("/systems/{systemCode}/subjects/{code}")
    public Subject updateSubject(@PathVariable String systemCode, @PathVariable String code,
            @Valid @RequestBody SubjectUpsertRequest request) {
        return reference.updateSubject(systemCode, code, request.toDomain());
    }

    @PostMapping("/systems/{systemCode}/subjects/{code}/archived")
    public Subject setSubjectArchived(@PathVariable String systemCode, @PathVariable String code,
            @RequestParam boolean value) {
        return reference.setSubjectArchived(systemCode, code, value);
    }
}
