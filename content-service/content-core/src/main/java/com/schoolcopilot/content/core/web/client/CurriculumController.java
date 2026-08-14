package com.schoolcopilot.content.core.web.client;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.content.core.service.ChapterService;
import com.schoolcopilot.content.core.web.client.dto.ChapterView;

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

    public CurriculumController(ChapterService chapters) {
        this.chapters = chapters;
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
}
