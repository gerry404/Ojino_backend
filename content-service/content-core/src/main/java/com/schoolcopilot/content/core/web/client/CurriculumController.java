package com.schoolcopilot.content.core.web.client;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.content.core.domain.Difficulty;
import com.schoolcopilot.content.core.service.ChapterService;
import com.schoolcopilot.content.core.service.ExerciseService;
import com.schoolcopilot.content.core.service.LearningResourceService;
import com.schoolcopilot.content.core.service.NotionService;
import com.schoolcopilot.content.core.web.client.dto.ChapterView;
import com.schoolcopilot.content.core.web.client.dto.ExerciseView;
import com.schoolcopilot.content.core.web.client.dto.NotionView;
import com.schoolcopilot.content.core.web.client.dto.ResourceView;

/**
 * Le programme : chapitres et, bientot, notions.
 *
 * <p>Prefixe distinct de {@code /api/v1/reference} a dessein. Le referentiel dit
 * <em>ou</em> se situe l'eleve — systeme, niveau, filiere. Le programme dit
 * <em>ce qu'il apprend</em>. Deux choses de nature et de rythme differents : la
 * taxonomie bouge une fois par reforme, le contenu tous les jours.
 *
 * <p>Publique et en lecture seule. Seuls les chapitres publies y apparaissent.
 */
@RestController
@RequestMapping("/api/v1/curriculum")
public class CurriculumController {

    private final ChapterService chapters;
    private final NotionService notions;
    private final LearningResourceService resources;
    private final ExerciseService exercises;

    public CurriculumController(ChapterService chapters, NotionService notions,
            LearningResourceService resources, ExerciseService exercises) {
        this.chapters = chapters;
        this.notions = notions;
        this.resources = resources;
        this.exercises = exercises;
    }

    /**
     * Les chapitres d'une classe.
     *
     * @param anchor facultatif : matiere, domaine d'apprentissage ou unite
     *        d'enseignement, selon le cycle
     * @param track facultatif : ecarte les chapitres reserves a une autre filiere
     */
    @GetMapping("/systems/{systemCode}/levels/{levelCode}/chapters")
    public List<ChapterView> chapters(
            @PathVariable String systemCode,
            @PathVariable String levelCode,
            @RequestParam(required = false) String anchor,
            @RequestParam(required = false) String track) {

        return chapters.visibleFor(systemCode, levelCode, anchor, track).stream()
                .map(ChapterView::from)
                .toList();
    }

    @GetMapping("/systems/{systemCode}/chapters/{code}")
    public ChapterView chapter(@PathVariable String systemCode, @PathVariable String code) {
        return ChapterView.from(chapters.requireVisible(systemCode, code));
    }

    // ------------------------------------------------------------------
    // Notions
    // ------------------------------------------------------------------

    @GetMapping("/systems/{systemCode}/chapters/{chapterCode}/notions")
    public List<NotionView> notions(@PathVariable String systemCode,
            @PathVariable String chapterCode) {
        return notions.visibleFor(systemCode, chapterCode).stream()
                .map(NotionView::from)
                .toList();
    }

    @GetMapping("/systems/{systemCode}/notions/{code}")
    public NotionView notion(@PathVariable String systemCode, @PathVariable String code) {
        return NotionView.from(notions.requireVisible(systemCode, code));
    }

    /**
     * Le parcours de rattrapage : tout ce qu'il faut maitriser avant cette notion,
     * dans l'ordre ou le reprendre.
     *
     * <p>C'est ce que consommera {@code learning-service} pour proposer une
     * remediation a un eleve qui bloque, plutot que de lui faire tout recommencer.
     */
    @GetMapping("/systems/{systemCode}/notions/{code}/learning-path")
    public List<NotionView> learningPath(@PathVariable String systemCode,
            @PathVariable String code) {
        return notions.learningPath(systemCode, code).stream().map(NotionView::from).toList();
    }

    /** L'autre sens du graphe : ce que la maitrise de cette notion ouvre. */
    @GetMapping("/systems/{systemCode}/notions/{code}/unlocks")
    public List<NotionView> unlocks(@PathVariable String systemCode, @PathVariable String code) {
        return notions.unlockedBy(systemCode, code).stream().map(NotionView::from).toList();
    }

    // ------------------------------------------------------------------
    // Supports et exercices
    // ------------------------------------------------------------------

    @GetMapping("/systems/{systemCode}/notions/{notionCode}/resources")
    public List<ResourceView> resources(@PathVariable String systemCode,
            @PathVariable String notionCode) {
        return resources.visibleFor(systemCode, notionCode).stream()
                .map(ResourceView::from)
                .toList();
    }

    /**
     * @param difficulty facultatif : permet de proposer d'abord du facile a
     *        quelqu'un qui vient d'echouer
     */
    @GetMapping("/systems/{systemCode}/notions/{notionCode}/exercises")
    public List<ExerciseView> exercises(@PathVariable String systemCode,
            @PathVariable String notionCode,
            @RequestParam(required = false) Difficulty difficulty) {
        return exercises.visibleFor(systemCode, notionCode, difficulty).stream()
                .map(ExerciseView::from)
                .toList();
    }

    /** L'enonce seul : {@link ExerciseView} ne porte aucun corrige. */
    @GetMapping("/systems/{systemCode}/exercises/{code}")
    public ExerciseView exercise(@PathVariable String systemCode, @PathVariable String code) {
        return ExerciseView.from(exercises.requireVisible(systemCode, code));
    }

    /**
     * Le corrige, par une route dediee.
     *
     * <p>Ouverte aujourd'hui ; elle sera conditionnee a une tentative quand
     * {@code learning-service} existera. La separation est en place des maintenant
     * pour que ce durcissement ne demande de toucher a aucun appelant.
     */
    @GetMapping("/systems/{systemCode}/exercises/{code}/solution")
    public Map<String, String> solution(@PathVariable String systemCode,
            @PathVariable String code) {
        return Map.of("solution", exercises.solutionOf(systemCode, code));
    }
}
