package com.schoolcopilot.content_service.service.reference;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.schoolcopilot.content_service.domain.reference.EducationLevel;
import com.schoolcopilot.content_service.domain.reference.EducationSystem;
import com.schoolcopilot.content_service.domain.reference.Subject;
import com.schoolcopilot.content_service.domain.reference.Track;
import com.schoolcopilot.content_service.exception.ApiException;
import com.schoolcopilot.content_service.repository.ReferenceRepositories;

/**
 * Le pilotage du referentiel scolaire par l'equipe.
 *
 * <p><strong>Rien ne se supprime, tout s'archive.</strong> Un element archive
 * disparait des choix proposes mais reste resolvable, donc un profil deja
 * rattache continue de fonctionner.
 *
 * <p>Ce choix remplace le controle d'usage qui existait quand le referentiel
 * vivait dans le meme service que les profils. Le maintenir aurait impose a ce
 * service d'interroger {@code user-service} avant chaque suppression, alors que
 * {@code user-service} l'interroge deja pour valider les choix des eleves : deux
 * services qui s'appellent mutuellement ne peuvent plus etre deployes ni testes
 * separement. L'archivage rend la question sans objet.
 */
@Service
public class AdminReferenceService {

    private static final Logger log = LoggerFactory.getLogger(AdminReferenceService.class);

    private final ReferenceRepositories.EducationSystems systems;
    private final ReferenceRepositories.EducationLevels levels;
    private final ReferenceRepositories.Tracks tracks;
    private final ReferenceRepositories.Subjects subjects;

    public AdminReferenceService(ReferenceRepositories.EducationSystems systems,
            ReferenceRepositories.EducationLevels levels,
            ReferenceRepositories.Tracks tracks,
            ReferenceRepositories.Subjects subjects) {
        this.systems = systems;
        this.levels = levels;
        this.tracks = tracks;
        this.subjects = subjects;
    }

    // ------------------------------------------------------------------
    // Systemes
    // ------------------------------------------------------------------

    /** Inclut les systemes desactives, que la route publique masque. */
    public List<EducationSystem> listSystems() {
        return systems.findAll(Sort.by("displayOrder"));
    }

    public EducationSystem requireSystem(String code) {
        return systems.findById(code).orElseThrow(() -> ApiException.unknownSystem(code));
    }

    public EducationSystem createSystem(EducationSystem system) {
        String code = normalizeCode(system.code());
        if (systems.existsById(code)) {
            throw ApiException.alreadyExists("Le systeme", code);
        }
        log.info("Systeme scolaire {} cree.", code);
        return systems.save(new EducationSystem(code, system.country(), system.countryLabel(),
                system.label(), system.language(), system.displayOrder(), system.active()));
    }

    public EducationSystem updateSystem(String code, EducationSystem changes) {
        requireSystem(code);
        return systems.save(new EducationSystem(code, changes.country(), changes.countryLabel(),
                changes.label(), changes.language(), changes.displayOrder(), changes.active()));
    }

    /** Desactive ou reactive un systeme entier. */
    public EducationSystem setSystemActive(String code, boolean active) {
        EducationSystem system = requireSystem(code);
        log.info("Systeme scolaire {} {}.", code, active ? "reactive" : "desactive");
        return systems.save(new EducationSystem(system.code(), system.country(),
                system.countryLabel(), system.label(), system.language(), system.displayOrder(),
                active));
    }

    // ------------------------------------------------------------------
    // Niveaux
    // ------------------------------------------------------------------

    /** Inclut les niveaux archives, que la route publique masque. */
    public List<EducationLevel> listLevels(String systemCode) {
        requireSystem(systemCode);
        return levels.findBySystemCodeOrderByRankAsc(systemCode);
    }

    public EducationLevel createLevel(String systemCode, EducationLevel level) {
        requireSystem(systemCode);
        String code = normalizeCode(level.code());
        levels.findBySystemCodeAndCode(systemCode, code).ifPresent(existing -> {
            throw ApiException.alreadyExists("Le niveau", code);
        });
        validateAgeRange(level);
        return levels.save(new EducationLevel(identity(systemCode, code), systemCode, code,
                level.label(), level.cycle(), level.rank(), level.typicalAgeMin(),
                level.typicalAgeMax(), level.hasTracks(), false));
    }

    public EducationLevel updateLevel(String systemCode, String code, EducationLevel changes) {
        EducationLevel existing = requireLevel(systemCode, code);
        validateAgeRange(changes);
        return levels.save(new EducationLevel(existing.id(), systemCode, code, changes.label(),
                changes.cycle(), changes.rank(), changes.typicalAgeMin(), changes.typicalAgeMax(),
                changes.hasTracks(), existing.archived()));
    }

    public EducationLevel setLevelArchived(String systemCode, String code, boolean archived) {
        EducationLevel level = requireLevel(systemCode, code);
        log.info("Niveau {} du systeme {} {}.", code, systemCode,
                archived ? "archive" : "desarchive");
        return levels.save(new EducationLevel(level.id(), level.systemCode(), level.code(),
                level.label(), level.cycle(), level.rank(), level.typicalAgeMin(),
                level.typicalAgeMax(), level.hasTracks(), archived));
    }

    // ------------------------------------------------------------------
    // Filieres
    // ------------------------------------------------------------------

    public List<Track> listTracks(String systemCode) {
        requireSystem(systemCode);
        return tracks.findBySystemCodeOrderByDisplayOrderAsc(systemCode);
    }

    public Track createTrack(String systemCode, Track track) {
        requireSystem(systemCode);
        String code = normalizeCode(track.code());
        tracks.findBySystemCodeAndCode(systemCode, code).ifPresent(existing -> {
            throw ApiException.alreadyExists("La filiere", code);
        });
        requireKnownLevels(systemCode, track.levelCodes());
        return tracks.save(new Track(identity(systemCode, code), systemCode, code, track.label(),
                track.description(), track.levelCodes(), track.displayOrder(), false));
    }

    public Track updateTrack(String systemCode, String code, Track changes) {
        Track existing = requireTrack(systemCode, code);
        requireKnownLevels(systemCode, changes.levelCodes());
        return tracks.save(new Track(existing.id(), systemCode, code, changes.label(),
                changes.description(), changes.levelCodes(), changes.displayOrder(),
                existing.archived()));
    }

    public Track setTrackArchived(String systemCode, String code, boolean archived) {
        Track track = requireTrack(systemCode, code);
        log.info("Filiere {} du systeme {} {}.", code, systemCode,
                archived ? "archivee" : "desarchivee");
        return tracks.save(new Track(track.id(), track.systemCode(), track.code(), track.label(),
                track.description(), track.levelCodes(), track.displayOrder(), archived));
    }

    // ------------------------------------------------------------------
    // Matieres
    // ------------------------------------------------------------------

    public List<Subject> listSubjects(String systemCode) {
        requireSystem(systemCode);
        return subjects.findBySystemCodeOrderByDisplayOrderAsc(systemCode);
    }

    public Subject createSubject(String systemCode, Subject subject) {
        requireSystem(systemCode);
        String code = normalizeCode(subject.code());
        subjects.findBySystemCodeAndCode(systemCode, code).ifPresent(existing -> {
            throw ApiException.alreadyExists("La matiere", code);
        });
        requireKnownLevels(systemCode, subject.levelCodes());
        return subjects.save(new Subject(identity(systemCode, code), systemCode, code,
                subject.label(), subject.levelCodes(), subject.trackCodes(), subject.core(),
                subject.displayOrder(), false));
    }

    public Subject updateSubject(String systemCode, String code, Subject changes) {
        Subject existing = requireSubject(systemCode, code);
        requireKnownLevels(systemCode, changes.levelCodes());
        return subjects.save(new Subject(existing.id(), systemCode, code, changes.label(),
                changes.levelCodes(), changes.trackCodes(), changes.core(), changes.displayOrder(),
                existing.archived()));
    }

    public Subject setSubjectArchived(String systemCode, String code, boolean archived) {
        Subject subject = requireSubject(systemCode, code);
        log.info("Matiere {} du systeme {} {}.", code, systemCode,
                archived ? "archivee" : "desarchivee");
        return subjects.save(new Subject(subject.id(), subject.systemCode(), subject.code(),
                subject.label(), subject.levelCodes(), subject.trackCodes(), subject.core(),
                subject.displayOrder(), archived));
    }

    // ------------------------------------------------------------------

    private EducationLevel requireLevel(String systemCode, String code) {
        return levels.findBySystemCodeAndCode(systemCode, code)
                .orElseThrow(() -> ApiException.unknownLevel(code));
    }

    private Track requireTrack(String systemCode, String code) {
        return tracks.findBySystemCodeAndCode(systemCode, code)
                .orElseThrow(() -> ApiException.unknownTrack(code));
    }

    private Subject requireSubject(String systemCode, String code) {
        return subjects.findBySystemCodeAndCode(systemCode, code)
                .orElseThrow(() -> ApiException.unknownSubjects(code));
    }

    /**
     * Les codes de niveau cites par une filiere ou une matiere doivent exister,
     * sinon la restriction ne s'appliquerait jamais et l'element resterait
     * invisible sans que personne comprenne pourquoi.
     */
    private void requireKnownLevels(String systemCode, List<String> levelCodes) {
        if (levelCodes == null || levelCodes.isEmpty()) {
            return;
        }
        Set<String> known = levels.findBySystemCodeOrderByRankAsc(systemCode).stream()
                .map(EducationLevel::code)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        Set<String> unknown = new LinkedHashSet<>(levelCodes);
        unknown.removeAll(known);
        if (!unknown.isEmpty()) {
            throw ApiException.unknownLevelsReferenced(unknown);
        }
    }

    private void validateAgeRange(EducationLevel level) {
        if (level.typicalAgeMin() > level.typicalAgeMax()) {
            throw ApiException.invalidAgeRange();
        }
    }

    /** Meme convention que le chargement initial : recharger ne cree pas de doublon. */
    private String identity(String systemCode, String code) {
        return systemCode + ":" + code;
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }
}
