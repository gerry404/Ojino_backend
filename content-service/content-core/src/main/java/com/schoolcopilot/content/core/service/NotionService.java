package com.schoolcopilot.content.core.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.schoolcopilot.content.core.domain.Chapter;
import com.schoolcopilot.content.core.domain.Notion;
import com.schoolcopilot.content.core.domain.PublicationStatus;
import com.schoolcopilot.content.core.exception.ApiException;
import com.schoolcopilot.content.core.repository.CurriculumRepositories;

/**
 * Les notions et leur graphe de prerequis.
 *
 * <p>La notion est l'unite de maitrise : c'est a elle que se rattacheront la
 * progression, la revision espacee et le contenu sur lequel l'assistant
 * s'appuiera.
 *
 * <p>Le graphe est ce qui rend la remediation possible. Sans lui, on ne peut que
 * constater qu'un eleve echoue ; avec lui, on sait <em>quoi</em> lui faire
 * reprendre, et dans quel ordre.
 */
@Service
public class NotionService {

    private static final Logger log = LoggerFactory.getLogger(NotionService.class);

    private final CurriculumRepositories.Notions notions;
    private final ChapterService chapters;

    public NotionService(CurriculumRepositories.Notions notions, ChapterService chapters) {
        this.notions = notions;
        this.chapters = chapters;
    }

    // ------------------------------------------------------------------
    // Lecture applicative
    // ------------------------------------------------------------------

    /** Les notions publiees d'un chapitre publie. */
    public List<Notion> visibleFor(String systemCode, String chapterCode) {
        chapters.requireVisible(systemCode, chapterCode);
        return notions.findBySystemCodeAndChapterCodeOrderByRankAsc(systemCode, chapterCode)
                .stream()
                .filter(Notion::isVisible)
                .toList();
    }

    public Notion requireVisible(String systemCode, String code) {
        Notion notion = require(systemCode, code);
        if (!notion.isVisible()) {
            throw ApiException.unknownNotion(code);
        }
        return notion;
    }

    /**
     * Tout ce qu'il faut maitriser avant cette notion, dans l'ordre ou le
     * reprendre.
     *
     * <p>C'est le parcours de rattrapage : un eleve qui bloque sur les derivees
     * recoit les limites d'abord, puis ce qui en decoule. Les notions non publiees
     * sont ecartees du resultat, mais restent traversees — sinon une notion en
     * brouillon couperait la chaine et masquerait ses propres prerequis.
     */
    public List<Notion> learningPath(String systemCode, String code) {
        require(systemCode, code);

        List<Notion> all = notions.findBySystemCode(systemCode);
        Map<String, Notion> byCode = all.stream()
                .collect(java.util.stream.Collectors.toMap(Notion::code, Function.identity(),
                        (first, second) -> first));

        return NotionGraph.of(all).learningPath(code).stream()
                .map(byCode::get)
                .filter(java.util.Objects::nonNull)
                .filter(Notion::isVisible)
                .toList();
    }

    /** Les notions que celle-ci debloque : l'autre sens du graphe. */
    public List<Notion> unlockedBy(String systemCode, String code) {
        require(systemCode, code);
        return notions.findBySystemCodeAndPrerequisiteCodesContaining(systemCode, code).stream()
                .filter(Notion::isVisible)
                .toList();
    }

    // ------------------------------------------------------------------
    // Back-office
    // ------------------------------------------------------------------

    /** Inclut les brouillons et les archives. */
    public List<Notion> listAll(String systemCode, String chapterCode) {
        chapters.require(systemCode, chapterCode);
        return notions.findBySystemCodeAndChapterCodeOrderByRankAsc(systemCode, chapterCode);
    }

    public Notion require(String systemCode, String code) {
        return notions.findBySystemCodeAndCode(systemCode, code)
                .orElseThrow(() -> ApiException.unknownNotion(code));
    }

    /** Cree en brouillon, sans prerequis : ils se declarent par leur propre route. */
    public Notion create(String systemCode, String chapterCode, Notion draft) {
        Chapter chapter = chapters.require(systemCode, chapterCode);
        String code = normalize(draft.code());

        notions.findBySystemCodeAndCode(systemCode, code).ifPresent(existing -> {
            throw ApiException.alreadyExists("La notion", code);
        });

        Notion notion = new Notion(identity(systemCode, code), systemCode, chapter.code(), code,
                draft.label(), draft.summary(), draft.rank(), List.of(),
                PublicationStatus.DRAFT, false);

        log.info("Notion {} creee en brouillon (chapitre {}).", code, chapterCode);
        return notions.save(notion);
    }

    public Notion update(String systemCode, String code, Notion changes) {
        Notion existing = require(systemCode, code);
        return notions.save(new Notion(existing.id(), systemCode, existing.chapterCode(), code,
                changes.label(), changes.summary(), changes.rank(), existing.prerequisiteCodes(),
                existing.status(), existing.archived()));
    }

    /**
     * Remplace les prerequis d'une notion.
     *
     * <p>Trois verifications, dans cet ordre : les codes existent, la notion ne se
     * cite pas elle-meme, et l'ajout ne ferme pas de boucle dans le graphe.
     */
    public Notion setPrerequisites(String systemCode, String code, List<String> requested) {
        Notion notion = require(systemCode, code);

        List<String> candidates = List.copyOf(new LinkedHashSet<>(
                requested == null ? List.of() : requested));

        List<Notion> all = notions.findBySystemCode(systemCode);
        Set<String> known = all.stream()
                .map(Notion::code)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        Set<String> unknown = new LinkedHashSet<>(candidates);
        unknown.removeAll(known);
        if (!unknown.isEmpty()) {
            throw ApiException.unknownNotion(unknown);
        }

        if (NotionGraph.of(all).wouldCreateCycle(code, candidates)) {
            throw ApiException.prerequisiteCycle(code, candidates);
        }

        log.info("Prerequis de {} : {}", code, candidates);
        return notions.save(new Notion(notion.id(), systemCode, notion.chapterCode(), code,
                notion.label(), notion.summary(), notion.rank(), candidates, notion.status(),
                notion.archived()));
    }

    public Notion setStatus(String systemCode, String code, PublicationStatus status) {
        Notion notion = require(systemCode, code);
        return notions.save(withStatus(notion, status, notion.archived()));
    }

    public Notion setArchived(String systemCode, String code, boolean archived) {
        Notion notion = require(systemCode, code);
        return notions.save(withStatus(notion, notion.status(), archived));
    }

    // ------------------------------------------------------------------

    private Notion withStatus(Notion notion, PublicationStatus status, boolean archived) {
        return new Notion(notion.id(), notion.systemCode(), notion.chapterCode(), notion.code(),
                notion.label(), notion.summary(), notion.rank(), notion.prerequisiteCodes(),
                status, archived);
    }

    private String identity(String systemCode, String code) {
        return systemCode + ":" + code;
    }

    private String normalize(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }
}
