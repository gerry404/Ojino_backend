package com.schoolcopilot.content.core.web.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.content.core.domain.Chapter;
import com.schoolcopilot.content.core.domain.Notion;
import com.schoolcopilot.content.core.domain.PublicationStatus;
import com.schoolcopilot.content.core.service.ChapterService;
import com.schoolcopilot.content.core.service.NotionService;
import com.schoolcopilot.content.core.web.admin.dto.ChapterUpsertRequest;
import com.schoolcopilot.content.core.web.admin.dto.NotionUpsertRequest;

import jakarta.validation.Valid;

/**
 * Back-office du programme.
 *
 * <p>Les reponses renvoient le document tel quel, statut editorial compris : une
 * equipe pedagogique a precisement besoin de voir ce qui est encore en brouillon.
 */
@RestController
@RequestMapping("/api/v1/admin/curriculum")
public class AdminCurriculumController {

    private final ChapterService chapters;
    private final NotionService notions;

    public AdminCurriculumController(ChapterService chapters, NotionService notions) {
        this.chapters = chapters;
        this.notions = notions;
    }

    /** Inclut les brouillons et les archives, contrairement a la route publique. */
    @GetMapping("/systems/{systemCode}/levels/{levelCode}/chapters")
    public List<Chapter> list(@PathVariable String systemCode, @PathVariable String levelCode) {
        return chapters.listAll(systemCode, levelCode);
    }

    /** Cree toujours en brouillon. */
    @PostMapping("/systems/{systemCode}/levels/{levelCode}/chapters")
    @ResponseStatus(HttpStatus.CREATED)
    public Chapter create(@PathVariable String systemCode, @PathVariable String levelCode,
            @Valid @RequestBody ChapterUpsertRequest request) {
        return chapters.create(systemCode, levelCode, request.toDomain());
    }

    @PutMapping("/systems/{systemCode}/chapters/{code}")
    public Chapter update(@PathVariable String systemCode, @PathVariable String code,
            @Valid @RequestBody ChapterUpsertRequest request) {
        return chapters.update(systemCode, code, request.toDomain());
    }

    /** Publier ou remettre en brouillon. */
    @PostMapping("/systems/{systemCode}/chapters/{code}/status")
    public Chapter setStatus(@PathVariable String systemCode, @PathVariable String code,
            @RequestParam PublicationStatus value) {
        return chapters.setStatus(systemCode, code, value);
    }

    @PostMapping("/systems/{systemCode}/chapters/{code}/archived")
    public Chapter setArchived(@PathVariable String systemCode, @PathVariable String code,
            @RequestParam boolean value) {
        return chapters.setArchived(systemCode, code, value);
    }

    // ------------------------------------------------------------------
    // Notions
    // ------------------------------------------------------------------

    @GetMapping("/systems/{systemCode}/chapters/{chapterCode}/notions")
    public List<Notion> listNotions(@PathVariable String systemCode,
            @PathVariable String chapterCode) {
        return notions.listAll(systemCode, chapterCode);
    }

    @PostMapping("/systems/{systemCode}/chapters/{chapterCode}/notions")
    @ResponseStatus(HttpStatus.CREATED)
    public Notion createNotion(@PathVariable String systemCode, @PathVariable String chapterCode,
            @Valid @RequestBody NotionUpsertRequest request) {
        return notions.create(systemCode, chapterCode, request.toDomain());
    }

    @PutMapping("/systems/{systemCode}/notions/{code}")
    public Notion updateNotion(@PathVariable String systemCode, @PathVariable String code,
            @Valid @RequestBody NotionUpsertRequest request) {
        return notions.update(systemCode, code, request.toDomain());
    }

    /**
     * Remplace la liste complete des prerequis.
     *
     * <p>Route separee de la modification : declarer un prerequis demande de
     * verifier le graphe entier, et une correction de libelle n'a pas a echouer
     * sur un probleme de cycle.
     */
    @PutMapping("/systems/{systemCode}/notions/{code}/prerequisites")
    public Notion setPrerequisites(@PathVariable String systemCode, @PathVariable String code,
            @Valid @RequestBody NotionUpsertRequest.Prerequisites request) {
        return notions.setPrerequisites(systemCode, code, request.codes());
    }

    @PostMapping("/systems/{systemCode}/notions/{code}/status")
    public Notion setNotionStatus(@PathVariable String systemCode, @PathVariable String code,
            @RequestParam PublicationStatus value) {
        return notions.setStatus(systemCode, code, value);
    }

    @PostMapping("/systems/{systemCode}/notions/{code}/archived")
    public Notion setNotionArchived(@PathVariable String systemCode, @PathVariable String code,
            @RequestParam boolean value) {
        return notions.setArchived(systemCode, code, value);
    }
}
