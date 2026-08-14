package com.schoolcopilot.content.core.service;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.schoolcopilot.content.core.domain.EducationCycle;
import com.schoolcopilot.content.core.domain.EducationLevel;
import com.schoolcopilot.content.core.domain.EducationSystem;
import com.schoolcopilot.content.core.domain.Subject;
import com.schoolcopilot.content.core.domain.Track;
import com.schoolcopilot.content.core.exception.ApiException;
import com.schoolcopilot.content.core.repository.ReferenceRepositories;
import com.schoolcopilot.content.core.spi.CurriculumModule;
import com.schoolcopilot.content.core.spi.CurriculumModules;
import com.schoolcopilot.content.core.spi.CurriculumStep;

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
 *
 * <p>Ce service ne connait aucun cycle en particulier : il interroge
 * {@link CurriculumModules} pour savoir ce que chacun demande.
 */
@Service
public class ReferenceService {

    private final ReferenceRepositories.EducationSystems systems;
    private final ReferenceRepositories.EducationLevels levels;
    private final ReferenceRepositories.Tracks tracks;
    private final ReferenceRepositories.Subjects subjects;
    private final CurriculumModules curriculumModules;

    public ReferenceService(ReferenceRepositories.EducationSystems systems,
            ReferenceRepositories.EducationLevels levels,
            ReferenceRepositories.Tracks tracks,
            ReferenceRepositories.Subjects subjects,
            CurriculumModules curriculumModules) {
        this.systems = systems;
        this.levels = levels;
        this.tracks = tracks;
        this.subjects = subjects;
        this.curriculumModules = curriculumModules;
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

    // ------------------------------------------------------------------
    // Cycles
    // ------------------------------------------------------------------

    /**
     * Les cycles que ce systeme propose et que ce deploiement sait servir.
     *
     * <p>Un cycle present dans les donnees mais dont le module Maven n'est pas
     * embarque est volontairement masque : mieux vaut ne rien proposer qu'ouvrir
     * un parcours que l'application ne saura pas mener a bout.
     */
    public List<CurriculumModule> cyclesOf(String systemCode) {
        requireSystem(systemCode);
        Set<EducationCycle> present = levels.findBySystemCodeOrderByRankAsc(systemCode).stream()
                .filter(level -> !level.archived())
                .map(EducationLevel::cycle)
                .collect(java.util.stream.Collectors.toCollection(
                        () -> java.util.EnumSet.noneOf(EducationCycle.class)));

        return curriculumModules.all().stream()
                .filter(module -> present.contains(module.cycle()))
                .toList();
    }

    /** Les choix que ce cycle demande. Vide si le module n'est pas embarque. */
    public List<CurriculumStep> stepsFor(EducationCycle cycle) {
        return curriculumModules.forCycle(cycle)
                .map(CurriculumModule::steps)
                .orElseGet(List::of);
    }

    // ------------------------------------------------------------------
    // Niveaux
    // ------------------------------------------------------------------

    /**
     * Les niveaux selectionnables du systeme, dans l'ordre, chacun marque ou non
     * comme suggere.
     *
     * <p>Sans age, rien n'est suggere. Avec un age, ce sont les classes dont la
     * tranche habituelle le contient ; et si aucune ne correspond — un eleve de 22
     * ans qui reprend ses etudes — c'est la plus proche qui est mise en avant,
     * pour ne jamais laisser l'ecran sans proposition.
     *
     * @param cycle facultatif, restreint a un seul cycle
     */
    public List<SuggestedLevel> levelsFor(String systemCode, Integer age, EducationCycle cycle) {
        requireSystem(systemCode);
        List<EducationLevel> all = levels.findBySystemCodeOrderByRankAsc(systemCode).stream()
                .filter(level -> !level.archived())
                .filter(level -> cycle == null || level.cycle() == cycle)
                // Un cycle dont le module n'est pas embarque ne doit pas etre propose.
                .filter(level -> curriculumModules.supports(level.cycle()))
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
        if (!curriculumModules.supports(level.cycle())) {
            throw ApiException.cycleNotAvailable(level.cycle().name());
        }
        return level;
    }

    // ------------------------------------------------------------------
    // Filieres
    // ------------------------------------------------------------------

    /** Les filieres proposees pour un niveau donne. Vide si le niveau n'en a pas. */
    public List<Track> tracksFor(String systemCode, String levelCode) {
        EducationLevel level = findLevel(systemCode, levelCode);
        if (!declares(level, CurriculumStep.TRACK)) {
            return List.of();
        }
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

    // ------------------------------------------------------------------
    // Matieres
    // ------------------------------------------------------------------

    /**
     * Les matieres pertinentes pour ce niveau et cette filiere.
     *
     * <p>Vide pour un cycle qui ne raisonne pas en matieres : la maternelle parle
     * de domaines d'apprentissage, l'universite d'unites d'enseignement. Sans ce
     * garde-fou, un enfant de petite section se verrait proposer la philosophie,
     * puisque les matieres sans restriction de niveau valent partout.
     */
    public List<Subject> subjectsFor(String systemCode, String levelCode, String trackCode) {
        requireSystem(systemCode);
        if (levelCode != null && !declares(findLevel(systemCode, levelCode),
                CurriculumStep.SUBJECTS)) {
            return List.of();
        }
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

    // ------------------------------------------------------------------

    private boolean declares(EducationLevel level, CurriculumStep step) {
        return stepsFor(level.cycle()).contains(step);
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
