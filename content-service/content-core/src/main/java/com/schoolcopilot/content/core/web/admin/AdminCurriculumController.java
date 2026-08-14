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
import com.schoolcopilot.content.core.domain.PublicationStatus;
import com.schoolcopilot.content.core.service.ChapterService;
import com.schoolcopilot.content.core.web.admin.dto.ChapterUpsertRequest;

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

    public AdminCurriculumController(ChapterService chapters) {
        this.chapters = chapters;
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
}
