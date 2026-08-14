package com.schoolcopilot.content.core.service;

import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.schoolcopilot.content.core.domain.Chapter;
import com.schoolcopilot.content.core.domain.EducationLevel;
import com.schoolcopilot.content.core.domain.PublicationStatus;
import com.schoolcopilot.content.core.exception.ApiException;
import com.schoolcopilot.content.core.repository.CurriculumRepositories;
import com.schoolcopilot.content.core.spi.CurriculumModule;
import com.schoolcopilot.content.core.spi.CurriculumModules;

/**
 * Le decoupage du programme en chapitres.
 *
 * <p>Deux lectures bien distinctes, et c'est volontaire :
 * <ul>
 *   <li>{@link #visibleFor} sert les applications — publie et non archive ;</li>
 *   <li>{@link #listAll} sert le back-office — tout, brouillons compris.</li>
 * </ul>
 * Melanger les deux derriere un booleen aurait fini par laisser fuiter un
 * brouillon vers un eleve le jour ou quelqu'un se trompe de valeur.
 */
@Service
public class ChapterService {

    private static final Logger log = LoggerFactory.getLogger(ChapterService.class);

    private final CurriculumRepositories.Chapters chapters;
    private final ReferenceService reference;
    private final CurriculumModules curriculumModules;

    public ChapterService(CurriculumRepositories.Chapters chapters, ReferenceService reference,
            CurriculumModules curriculumModules) {
        this.chapters = chapters;
        this.reference = reference;
        this.curriculumModules = curriculumModules;
    }

    // ------------------------------------------------------------------
    // Lecture applicative
    // ------------------------------------------------------------------

    /**
     * Les chapitres qu'un eleve doit voir.
     *
     * @param anchorCode facultatif : matiere, domaine ou unite d'enseignement
     * @param trackCode facultatif : ecarte les chapitres reserves a une autre filiere
     */
    public List<Chapter> visibleFor(String systemCode, String levelCode, String anchorCode,
            String trackCode) {
        reference.findLevel(systemCode, levelCode);

        List<Chapter> found = anchorCode == null
                ? chapters.findBySystemCodeAndLevelCodeOrderByRankAsc(systemCode, levelCode)
                : chapters.findBySystemCodeAndLevelCodeAndAnchorCodeOrderByRankAsc(
                        systemCode, levelCode, anchorCode);

        return found.stream()
                .filter(Chapter::isVisible)
                .filter(chapter -> chapter.appliesToTrack(trackCode))
                .toList();
    }

    /** Un chapitre publie. */
    public Chapter requireVisible(String systemCode, String code) {
        Chapter chapter = require(systemCode, code);
        if (!chapter.isVisible()) {
            throw ApiException.unknownChapter(code);
        }
        return chapter;
    }

    // ------------------------------------------------------------------
    // Back-office
    // ------------------------------------------------------------------

    /** Inclut les brouillons et les archives. */
    public List<Chapter> listAll(String systemCode, String levelCode) {
        reference.findLevel(systemCode, levelCode);
        return chapters.findBySystemCodeAndLevelCodeOrderByRankAsc(systemCode, levelCode);
    }

    public Chapter require(String systemCode, String code) {
        return chapters.findBySystemCodeAndCode(systemCode, code)
                .orElseThrow(() -> ApiException.unknownChapter(code));
    }

    /**
     * Cree un chapitre a l'etat de brouillon.
     *
     * <p>Jamais publie d'emblee : ecrire un chapitre et le rendre visible sont
     * deux decisions distinctes, prises a des moments differents.
     */
    public Chapter create(String systemCode, String levelCode, Chapter draft) {
        EducationLevel level = reference.requireSelectableLevel(systemCode, levelCode);
        String code = normalize(draft.code());

        chapters.findBySystemCodeAndCode(systemCode, code).ifPresent(existing -> {
            throw ApiException.alreadyExists("Le chapitre", code);
        });

        requireValidAnchor(systemCode, level, draft.anchorCode());
        requireValidTrack(systemCode, levelCode, draft.trackCode());

        Chapter chapter = new Chapter(identity(systemCode, code), systemCode, level.cycle(),
                levelCode, draft.anchorCode(), draft.trackCode(), code, draft.label(),
                draft.summary(), draft.rank(), PublicationStatus.DRAFT, false);

        log.info("Chapitre {} cree en brouillon ({} / {}).", code, systemCode, levelCode);
        return chapters.save(chapter);
    }

    /** Ne touche ni au statut ni a l'archivage : chacun a sa propre route. */
    public Chapter update(String systemCode, String code, Chapter changes) {
        Chapter existing = require(systemCode, code);
        EducationLevel level = reference.findLevel(systemCode, existing.levelCode());

        requireValidAnchor(systemCode, level, changes.anchorCode());
        requireValidTrack(systemCode, existing.levelCode(), changes.trackCode());

        return chapters.save(new Chapter(existing.id(), systemCode, existing.cycle(),
                existing.levelCode(), changes.anchorCode(), changes.trackCode(), code,
                changes.label(), changes.summary(), changes.rank(), existing.status(),
                existing.archived()));
    }

    public Chapter setStatus(String systemCode, String code, PublicationStatus status) {
        Chapter chapter = require(systemCode, code);
        log.info("Chapitre {} passe en {}.", code, status);
        return chapters.save(withStatus(chapter, status, chapter.archived()));
    }

    public Chapter setArchived(String systemCode, String code, boolean archived) {
        Chapter chapter = require(systemCode, code);
        return chapters.save(withStatus(chapter, chapter.status(), archived));
    }

    // ------------------------------------------------------------------

    /**
     * Delegue au module du cycle : lui seul sait si le code designe une matiere,
     * un domaine d'apprentissage ou une unite d'enseignement existante.
     */
    private void requireValidAnchor(String systemCode, EducationLevel level, String anchorCode) {
        CurriculumModule module = curriculumModules.forCycle(level.cycle())
                .orElseThrow(() -> ApiException.cycleNotAvailable(level.cycle().name()));

        if (anchorCode == null || !module.anchorExists(systemCode, anchorCode)) {
            throw ApiException.unknownAnchor(module.anchorKind().name(), anchorCode);
        }
    }

    /** Une filiere citee doit exister a ce niveau ; null vaut "toutes". */
    private void requireValidTrack(String systemCode, String levelCode, String trackCode) {
        if (trackCode != null) {
            reference.requireSelectableTrack(systemCode, levelCode, trackCode);
        }
    }

    private Chapter withStatus(Chapter chapter, PublicationStatus status, boolean archived) {
        return new Chapter(chapter.id(), chapter.systemCode(), chapter.cycle(),
                chapter.levelCode(), chapter.anchorCode(), chapter.trackCode(), chapter.code(),
                chapter.label(), chapter.summary(), chapter.rank(), status, archived);
    }

    private String identity(String systemCode, String code) {
        return systemCode + ":" + code;
    }

    private String normalize(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }
}
