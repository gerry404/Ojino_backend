package com.schoolcopilot.user_service.web.admin;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.user_service.domain.reference.EducationLevel;
import com.schoolcopilot.user_service.domain.reference.EducationSystem;
import com.schoolcopilot.user_service.domain.reference.Subject;
import com.schoolcopilot.user_service.domain.reference.Track;
import com.schoolcopilot.user_service.service.reference.AdminReferenceService;
import com.schoolcopilot.user_service.web.admin.dto.LevelUpsertRequest;
import com.schoolcopilot.user_service.web.admin.dto.SubjectUpsertRequest;
import com.schoolcopilot.user_service.web.admin.dto.SystemUpsertRequest;
import com.schoolcopilot.user_service.web.admin.dto.TrackUpsertRequest;

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

    /** Refuse si des profils s'y rattachent encore. */
    @DeleteMapping("/systems/{systemCode}/levels/{code}")
    public Map<String, String> deleteLevel(@PathVariable String systemCode,
            @PathVariable String code) {
        reference.deleteLevel(systemCode, code);
        return Map.of("status", "deleted");
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

    @DeleteMapping("/systems/{systemCode}/tracks/{code}")
    public Map<String, String> deleteTrack(@PathVariable String systemCode,
            @PathVariable String code) {
        reference.deleteTrack(systemCode, code);
        return Map.of("status", "deleted");
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

    @DeleteMapping("/systems/{systemCode}/subjects/{code}")
    public Map<String, String> deleteSubject(@PathVariable String systemCode,
            @PathVariable String code) {
        reference.deleteSubject(systemCode, code);
        return Map.of("status", "deleted");
    }
}
