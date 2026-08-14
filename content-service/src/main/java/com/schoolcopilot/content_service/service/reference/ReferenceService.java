package com.schoolcopilot.content_service.service.reference;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.schoolcopilot.content_service.domain.reference.EducationLevel;
import com.schoolcopilot.content_service.domain.reference.EducationSystem;
import com.schoolcopilot.content_service.domain.reference.Subject;
import com.schoolcopilot.content_service.domain.reference.Track;
import com.schoolcopilot.content_service.exception.ApiException;
import com.schoolcopilot.content_service.repository.ReferenceRepositories;

/**
 * Lecture du referentiel scolaire et suggestions.
 *
 * <p>Le service ne <em>filtre</em> jamais les niveaux selon l'age : il se contente
 * d'en <em>suggerer</em>. Un redoublement, une annee d'avance ou une reprise
 * d'etudes apres une interruption sont des situations normales, et une liste qui
 * les rendrait impossibles a saisir serait une liste cassee.
 *
 * <p>Distinction importante entre les deux familles de methodes :
 * <ul>
 *   <li>celles en {@code ...Selectable} excluent les elements archives — c'est ce
 *       qu'on propose a quelqu'un qui fait son choix ;</li>
 *   <li>celles en {@code find...} resolvent tout, archive compris — un profil deja
 *       rattache a un niveau retire doit continuer de s'afficher.</li>
 * </ul>
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
     * Tous les niveaux selectionnables du systeme, dans l'ordre, chacun marque ou
     * non comme suggere.
     *
     * <p>Sans age, rien n'est suggere. Avec un age, ce sont les classes dont la
     * tranche habituelle le contient ; et si aucune ne correspond — un eleve de 22
     * ans qui reprend ses etudes — c'est la plus proche qui est mise en avant,
     * pour ne jamais laisser l'ecran sans proposition.
     */
    public List<SuggestedLevel> levelsFor(String systemCode, Integer age) {
        requireSystem(systemCode);
        List<EducationLevel> all = levels.findBySystemCodeOrderByRankAsc(systemCode).stream()
                .filter(level -> !level.archived())
                .toList();

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

    /** Resout un niveau, archive compris : un profil existant doit rester lisible. */
    public EducationLevel findLevel(String systemCode, String levelCode) {
        return levels.findBySystemCodeAndCode(systemCode, levelCode)
                .orElseThrow(() -> ApiException.unknownLevel(levelCode));
    }

    /** Resout un niveau que l'on peut encore choisir. Refuse les archives. */
    public EducationLevel requireSelectableLevel(String systemCode, String levelCode) {
        EducationLevel level = findLevel(systemCode, levelCode);
        if (level.archived()) {
            throw ApiException.archived("Le niveau", levelCode);
        }
        return level;
    }

    /** Les filieres proposees pour un niveau donne. Vide si le niveau n'en a pas. */
    public List<Track> tracksFor(String systemCode, String levelCode) {
        findLevel(systemCode, levelCode);
        return tracks.findBySystemCodeOrderByDisplayOrderAsc(systemCode).stream()
                .filter(track -> !track.archived())
                .filter(track -> track.availableAt(levelCode))
                .toList();
    }

    public Track requireSelectableTrack(String systemCode, String levelCode, String trackCode) {
        Track track = tracks.findBySystemCodeAndCode(systemCode, trackCode)
                .orElseThrow(() -> ApiException.unknownTrack(trackCode));
        if (track.archived()) {
            throw ApiException.archived("La filiere", trackCode);
        }
        if (!track.availableAt(levelCode)) {
            throw ApiException.trackNotAvailable(trackCode, levelCode);
        }
        return track;
    }

    /** Les matieres pertinentes pour ce niveau et cette filiere. */
    public List<Subject> subjectsFor(String systemCode, String levelCode, String trackCode) {
        requireSystem(systemCode);
        return subjects.findBySystemCodeOrderByDisplayOrderAsc(systemCode).stream()
                .filter(subject -> !subject.archived())
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
