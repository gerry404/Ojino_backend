package com.schoolcopilot.user_service.service.reference;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.schoolcopilot.user_service.domain.reference.EducationLevel;
import com.schoolcopilot.user_service.domain.reference.EducationSystem;
import com.schoolcopilot.user_service.domain.reference.Subject;
import com.schoolcopilot.user_service.domain.reference.Track;
import com.schoolcopilot.user_service.exception.ApiException;
import com.schoolcopilot.user_service.repository.ReferenceRepositories;

/**
 * Lecture du referentiel scolaire et suggestions.
 *
 * <p>Le service ne <em>filtre</em> jamais les niveaux selon l'age : il se contente
 * d'en <em>suggerer</em>. Un redoublement, une annee d'avance ou une reprise
 * d'etudes apres une interruption sont des situations normales, et une liste qui
 * les rendrait impossibles a saisir serait une liste cassee.
 */
@Service
public class ReferenceService {

    private final ReferenceRepositories.EducationSystems systems;
    private final ReferenceRepositories.EducationLevels levels;
    private final ReferenceRepositories.Tracks tracks;
    private final ReferenceRepositories.Subjects subjects;

    public ReferenceService(ReferenceRepositories.EducationSystems systems,
            ReferenceRepositories.EducationLevels levels,
            ReferenceRepositories.Tracks tracks,
            ReferenceRepositories.Subjects subjects) {
        this.systems = systems;
        this.levels = levels;
        this.tracks = tracks;
        this.subjects = subjects;
    }

    /** Un niveau accompagne de son indication de pertinence pour l'age saisi. */
    public record SuggestedLevel(EducationLevel level, boolean suggested) {
    }

    public List<EducationSystem> listSystems() {
        return systems.findByActiveTrueOrderByDisplayOrderAsc();
    }

    public EducationSystem requireSystem(String systemCode) {
        return systems.findById(systemCode)
                .orElseThrow(() -> ApiException.unknownSystem(systemCode));
    }

    /**
     * Tous les niveaux du systeme, dans l'ordre, chacun marque ou non comme
     * suggere.
     *
     * <p>Sans age, rien n'est suggere. Avec un age, ce sont les classes dont la
     * tranche habituelle le contient ; et si aucune ne correspond — un eleve de 22
     * ans qui reprend ses etudes — c'est la plus proche qui est mise en avant,
     * pour ne jamais laisser l'ecran sans proposition.
     */
    public List<SuggestedLevel> levelsFor(String systemCode, Integer age) {
        requireSystem(systemCode);
        List<EducationLevel> all = levels.findBySystemCodeOrderByRankAsc(systemCode);

        if (age == null || all.isEmpty()) {
            return all.stream().map(level -> new SuggestedLevel(level, false)).toList();
        }

        boolean anyExactMatch = all.stream().anyMatch(level -> level.matchesAge(age));
        EducationLevel closest = anyExactMatch ? null : all.stream()
                .min(Comparator.comparingInt(level -> ageDistance(level, age)))
                .orElse(null);

        return all.stream()
                .map(level -> new SuggestedLevel(level,
                        anyExactMatch ? level.matchesAge(age) : level.equals(closest)))
                .toList();
    }

    public EducationLevel requireLevel(String systemCode, String levelCode) {
        return levels.findBySystemCodeAndCode(systemCode, levelCode)
                .orElseThrow(() -> ApiException.unknownLevel(levelCode));
    }

    /** Les filieres proposees pour un niveau donne. Vide si le niveau n'en a pas. */
    public List<Track> tracksFor(String systemCode, String levelCode) {
        requireLevel(systemCode, levelCode);
        return tracks.findBySystemCodeOrderByDisplayOrderAsc(systemCode).stream()
                .filter(track -> track.availableAt(levelCode))
                .toList();
    }

    public Track requireTrack(String systemCode, String levelCode, String trackCode) {
        Track track = tracks.findBySystemCodeAndCode(systemCode, trackCode)
                .orElseThrow(() -> ApiException.unknownTrack(trackCode));
        if (!track.availableAt(levelCode)) {
            throw ApiException.trackNotAvailable(trackCode, levelCode);
        }
        return track;
    }

    /** Les matieres pertinentes pour ce niveau et cette filiere. */
    public List<Subject> subjectsFor(String systemCode, String levelCode, String trackCode) {
        requireSystem(systemCode);
        return subjects.findBySystemCodeOrderByDisplayOrderAsc(systemCode).stream()
                .filter(subject -> subject.appliesTo(levelCode, trackCode))
                .toList();
    }

    /**
     * Verifie que chaque code choisi existe bien pour ce niveau et cette filiere,
     * et renvoie la selection debarrassee de ses doublons.
     */
    public List<String> validateSubjects(String systemCode, String levelCode, String trackCode,
            List<String> chosen) {
        Set<String> allowed = subjectsFor(systemCode, levelCode, trackCode).stream()
                .map(Subject::code)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        Set<String> unknown = new LinkedHashSet<>(chosen);
        unknown.removeAll(allowed);
        if (!unknown.isEmpty()) {
            throw ApiException.unknownSubjects(unknown);
        }
        return List.copyOf(new LinkedHashSet<>(chosen));
    }

    private int ageDistance(EducationLevel level, int age) {
        if (age < level.typicalAgeMin()) {
            return level.typicalAgeMin() - age;
        }
        if (age > level.typicalAgeMax()) {
            return age - level.typicalAgeMax();
        }
        return 0;
    }
}
