package com.schoolcopilot.user_service.service.reference;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.schoolcopilot.user_service.domain.reference.EducationLevel;
import com.schoolcopilot.user_service.domain.reference.EducationSystem;
import com.schoolcopilot.user_service.domain.reference.Subject;
import com.schoolcopilot.user_service.domain.reference.Track;
import com.schoolcopilot.user_service.exception.ApiException;
import com.schoolcopilot.user_service.repository.ReferenceRepositories;
import com.schoolcopilot.user_service.repository.StudentProfileRepository;

/**
 * Le pilotage du referentiel scolaire par l'equipe.
 *
 * <p>C'est ce qui rend le referentiel reellement configurable : sans ces routes,
 * ouvrir l'application a un nouveau pays supposerait de modifier Mongo a la main.
 *
 * <p>Deux regles gouvernent les suppressions. Un systeme ne se supprime pas, il se
 * desactive : les profils qui s'y rattachent doivent rester lisibles. Un niveau,
 * une filiere ou une matiere ne se supprime que s'il n'est plus reference par
 * aucun profil, sinon on laisserait des profils pointant vers un code disparu.
 */
@Service
public class AdminReferenceService {

    private static final Logger log = LoggerFactory.getLogger(AdminReferenceService.class);

    private final ReferenceRepositories.EducationSystems systems;
    private final ReferenceRepositories.EducationLevels levels;
    private final ReferenceRepositories.Tracks tracks;
    private final ReferenceRepositories.Subjects subjects;
    private final StudentProfileRepository profiles;

    public AdminReferenceService(ReferenceRepositories.EducationSystems systems,
            ReferenceRepositories.EducationLevels levels,
            ReferenceRepositories.Tracks tracks,
            ReferenceRepositories.Subjects subjects,
            StudentProfileRepository profiles) {
        this.systems = systems;
        this.levels = levels;
        this.tracks = tracks;
        this.subjects = subjects;
        this.profiles = profiles;
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
        EducationSystem created = new EducationSystem(code, system.country(),
                system.countryLabel(), system.label(), system.language(), system.displayOrder(),
                system.active());
        log.info("Systeme scolaire {} cree.", code);
        return systems.save(created);
    }

    public EducationSystem updateSystem(String code, EducationSystem changes) {
        requireSystem(code);
        return systems.save(new EducationSystem(code, changes.country(), changes.countryLabel(),
                changes.label(), changes.language(), changes.displayOrder(), changes.active()));
    }

    /**
     * Un systeme ne se supprime jamais : les profils qui s'y rattachent doivent
     * rester lisibles. Le desactiver le retire simplement des choix proposes.
     */
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
        return levels.save(withIdentity(systemCode, code, level));
    }

    public EducationLevel updateLevel(String systemCode, String code, EducationLevel changes) {
        EducationLevel existing = levels.findBySystemCodeAndCode(systemCode, code)
                .orElseThrow(() -> ApiException.unknownLevel(code));
        validateAgeRange(changes);
        return levels.save(new EducationLevel(existing.id(), systemCode, code, changes.label(),
                changes.cycle(), changes.rank(), changes.typicalAgeMin(), changes.typicalAgeMax(),
                changes.hasTracks()));
    }

    public void deleteLevel(String systemCode, String code) {
        EducationLevel level = levels.findBySystemCodeAndCode(systemCode, code)
                .orElseThrow(() -> ApiException.unknownLevel(code));
        long inUse = profiles.countBySystemCodeAndLevelCode(systemCode, code);
        if (inUse > 0) {
            throw ApiException.referenceInUse("Le niveau", code, inUse);
        }
        levels.delete(level);
        log.info("Niveau {} supprime du systeme {}.", code, systemCode);
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
                track.description(), track.levelCodes(), track.displayOrder()));
    }

    public Track updateTrack(String systemCode, String code, Track changes) {
        Track existing = tracks.findBySystemCodeAndCode(systemCode, code)
                .orElseThrow(() -> ApiException.unknownTrack(code));
        requireKnownLevels(systemCode, changes.levelCodes());
        return tracks.save(new Track(existing.id(), systemCode, code, changes.label(),
                changes.description(), changes.levelCodes(), changes.displayOrder()));
    }

    public void deleteTrack(String systemCode, String code) {
        Track track = tracks.findBySystemCodeAndCode(systemCode, code)
                .orElseThrow(() -> ApiException.unknownTrack(code));
        long inUse = profiles.countBySystemCodeAndTrackCode(systemCode, code);
        if (inUse > 0) {
            throw ApiException.referenceInUse("La filiere", code, inUse);
        }
        tracks.delete(track);
        log.info("Filiere {} supprimee du systeme {}.", code, systemCode);
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
                subject.displayOrder()));
    }

    public Subject updateSubject(String systemCode, String code, Subject changes) {
        Subject existing = subjects.findBySystemCodeAndCode(systemCode, code)
                .orElseThrow(() -> ApiException.unknownSubjects(code));
        requireKnownLevels(systemCode, changes.levelCodes());
        return subjects.save(new Subject(existing.id(), systemCode, code, changes.label(),
                changes.levelCodes(), changes.trackCodes(), changes.core(),
                changes.displayOrder()));
    }

    public void deleteSubject(String systemCode, String code) {
        Subject subject = subjects.findBySystemCodeAndCode(systemCode, code)
                .orElseThrow(() -> ApiException.unknownSubjects(code));
        long inUse = profiles.countBySystemCodeAndSubjectCodesContaining(systemCode, code);
        if (inUse > 0) {
            throw ApiException.referenceInUse("La matiere", code, inUse);
        }
        subjects.delete(subject);
        log.info("Matiere {} supprimee du systeme {}.", code, systemCode);
    }

    // ------------------------------------------------------------------

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

    private EducationLevel withIdentity(String systemCode, String code, EducationLevel level) {
        return new EducationLevel(identity(systemCode, code), systemCode, code, level.label(),
                level.cycle(), level.rank(), level.typicalAgeMin(), level.typicalAgeMax(),
                level.hasTracks());
    }

    /** Meme convention que le chargement initial : recharger ne cree pas de doublon. */
    private String identity(String systemCode, String code) {
        return systemCode + ":" + code;
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }
}
