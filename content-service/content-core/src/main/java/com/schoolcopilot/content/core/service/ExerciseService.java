package com.schoolcopilot.content.core.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.schoolcopilot.content.core.domain.Difficulty;
import com.schoolcopilot.content.core.domain.Exercise;
import com.schoolcopilot.content.core.domain.PublicationStatus;
import com.schoolcopilot.content.core.exception.ApiException;
import com.schoolcopilot.content.core.repository.CurriculumRepositories;

/**
 * Les exercices d'une notion.
 *
 * <p>Le corrige est traite a part de bout en bout : la vue applicative ne le
 * porte pas, et il faut une route dediee pour l'obtenir. Aujourd'hui elle est
 * ouverte ; elle sera conditionnee a une tentative quand {@code learning-service}
 * existera. Le point important est que la separation soit deja en place — la
 * rajouter apres coup demanderait de reprendre chaque appelant.
 */
@Service
public class ExerciseService {

    private final CurriculumRepositories.Exercises exercises;
    private final NotionService notions;

    public ExerciseService(CurriculumRepositories.Exercises exercises, NotionService notions) {
        this.exercises = exercises;
        this.notions = notions;
    }

    /**
     * Les exercices publies d'une notion publiee.
     *
     * @param difficulty facultatif : permet de proposer d'abord du facile a
     *        quelqu'un qui vient d'echouer
     */
    public List<Exercise> visibleFor(String systemCode, String notionCode, Difficulty difficulty) {
        notions.requireVisible(systemCode, notionCode);

        List<Exercise> found = difficulty == null
                ? exercises.findBySystemCodeAndNotionCodeOrderByDifficultyAscCodeAsc(
                        systemCode, notionCode)
                : exercises.findBySystemCodeAndNotionCodeAndDifficultyOrderByCodeAsc(
                        systemCode, notionCode, difficulty);

        return found.stream().filter(Exercise::isVisible).toList();
    }

    public Exercise requireVisible(String systemCode, String code) {
        Exercise exercise = require(systemCode, code);
        if (!exercise.isVisible()) {
            throw ApiException.unknownExercise(code);
        }
        return exercise;
    }

    /** Le corrige seul. Route distincte pour qu'il ne parte jamais avec l'enonce. */
    public String solutionOf(String systemCode, String code) {
        return requireVisible(systemCode, code).solution();
    }

    /** Total indicatif du temps de travail d'une notion, pour le planificateur. */
    public int estimatedMinutesFor(String systemCode, String notionCode) {
        return visibleFor(systemCode, notionCode, null).stream()
                .mapToInt(Exercise::estimatedMinutes)
                .sum();
    }

    // ------------------------------------------------------------------
    // Back-office
    // ------------------------------------------------------------------

    public List<Exercise> listAll(String systemCode, String notionCode) {
        notions.require(systemCode, notionCode);
        return exercises.findBySystemCodeAndNotionCodeOrderByDifficultyAscCodeAsc(systemCode,
                notionCode);
    }

    public Exercise require(String systemCode, String code) {
        return exercises.findBySystemCodeAndCode(systemCode, code)
                .orElseThrow(() -> ApiException.unknownExercise(code));
    }

    public Exercise create(String systemCode, String notionCode, Exercise draft) {
        notions.require(systemCode, notionCode);
        String code = normalize(draft.code());

        exercises.findBySystemCodeAndCode(systemCode, code).ifPresent(existing -> {
            throw ApiException.alreadyExists("L'exercice", code);
        });

        return exercises.save(new Exercise(identity(systemCode, code), systemCode, notionCode,
                code, draft.statement(), draft.solution(), draft.difficulty(),
                draft.estimatedMinutes(), PublicationStatus.DRAFT, false));
    }

    public Exercise update(String systemCode, String code, Exercise changes) {
        Exercise existing = require(systemCode, code);
        return exercises.save(new Exercise(existing.id(), systemCode, existing.notionCode(), code,
                changes.statement(), changes.solution(), changes.difficulty(),
                changes.estimatedMinutes(), existing.status(), existing.archived()));
    }

    public Exercise setStatus(String systemCode, String code, PublicationStatus status) {
        Exercise exercise = require(systemCode, code);
        return exercises.save(with(exercise, status, exercise.archived()));
    }

    public Exercise setArchived(String systemCode, String code, boolean archived) {
        Exercise exercise = require(systemCode, code);
        return exercises.save(with(exercise, exercise.status(), archived));
    }

    // ------------------------------------------------------------------

    private Exercise with(Exercise exercise, PublicationStatus status, boolean archived) {
        return new Exercise(exercise.id(), exercise.systemCode(), exercise.notionCode(),
                exercise.code(), exercise.statement(), exercise.solution(), exercise.difficulty(),
                exercise.estimatedMinutes(), status, archived);
    }

    private String identity(String systemCode, String code) {
        return systemCode + ":" + code;
    }

    private String normalize(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }
}
