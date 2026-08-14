package com.schoolcopilot.content.core.web.client;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.content.core.service.ChapterService;
import com.schoolcopilot.content.core.service.NotionService;
import com.schoolcopilot.content.core.web.client.dto.ChapterView;
import com.schoolcopilot.content.core.web.client.dto.NotionView;

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

    public CurriculumController(ChapterService chapters, NotionService notions) {
        this.chapters = chapters;
        this.notions = notions;
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
}
