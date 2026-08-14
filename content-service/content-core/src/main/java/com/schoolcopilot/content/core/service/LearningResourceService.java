package com.schoolcopilot.content.core.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.schoolcopilot.content.core.domain.LearningResource;
import com.schoolcopilot.content.core.domain.PublicationStatus;
import com.schoolcopilot.content.core.domain.ResourceType;
import com.schoolcopilot.content.core.exception.ApiException;
import com.schoolcopilot.content.core.repository.CurriculumRepositories;

/** Les supports d'apprentissage : lecons, fiches, videos, liens. */
@Service
public class LearningResourceService {

    private final CurriculumRepositories.LearningResources resources;
    private final NotionService notions;

    public LearningResourceService(CurriculumRepositories.LearningResources resources,
            NotionService notions) {
        this.resources = resources;
        this.notions = notions;
    }

    /** Les supports publies d'une notion publiee. */
    public List<LearningResource> visibleFor(String systemCode, String notionCode) {
        notions.requireVisible(systemCode, notionCode);
        return resources.findBySystemCodeAndNotionCodeOrderByRankAsc(systemCode, notionCode)
                .stream()
                .filter(LearningResource::isVisible)
                .toList();
    }

    /** Inclut les brouillons et les archives. */
    public List<LearningResource> listAll(String systemCode, String notionCode) {
        notions.require(systemCode, notionCode);
        return resources.findBySystemCodeAndNotionCodeOrderByRankAsc(systemCode, notionCode);
    }

    public LearningResource require(String systemCode, String code) {
        return resources.findBySystemCodeAndCode(systemCode, code)
                .orElseThrow(() -> ApiException.unknownResource(code));
    }

    public LearningResource create(String systemCode, String notionCode, LearningResource draft) {
        notions.require(systemCode, notionCode);
        String code = normalize(draft.code());

        resources.findBySystemCodeAndCode(systemCode, code).ifPresent(existing -> {
            throw ApiException.alreadyExists("La ressource", code);
        });
        requireCoherentContent(draft);

        return resources.save(new LearningResource(identity(systemCode, code), systemCode,
                notionCode, code, draft.type(), draft.label(), draft.body(), draft.url(),
                draft.rank(), PublicationStatus.DRAFT, false));
    }

    public LearningResource update(String systemCode, String code, LearningResource changes) {
        LearningResource existing = require(systemCode, code);
        requireCoherentContent(changes);

        return resources.save(new LearningResource(existing.id(), systemCode,
                existing.notionCode(), code, changes.type(), changes.label(), changes.body(),
                changes.url(), changes.rank(), existing.status(), existing.archived()));
    }

    public LearningResource setStatus(String systemCode, String code, PublicationStatus status) {
        LearningResource resource = require(systemCode, code);
        return resources.save(with(resource, status, resource.archived()));
    }

    public LearningResource setArchived(String systemCode, String code, boolean archived) {
        LearningResource resource = require(systemCode, code);
        return resources.save(with(resource, resource.status(), archived));
    }

    /**
     * Une lecon sans texte, ou une video sans adresse, s'enregistrerait sans bruit
     * et n'afficherait rien du tout chez l'eleve. Autant le refuser tout de suite.
     */
    private void requireCoherentContent(LearningResource resource) {
        if (resource.type() == null) {
            throw ApiException.invalidResource("Le type de ressource est obligatoire.");
        }
        if (resource.type().isAuthored() && isBlank(resource.body())) {
            throw ApiException.invalidResource(
                    "Une lecon ou une fiche doit avoir un contenu redige.");
        }
        if (!resource.type().isAuthored() && isBlank(resource.url())) {
            throw ApiException.invalidResource(
                    "Une video ou un lien doit avoir une adresse.");
        }
    }

    private LearningResource with(LearningResource resource, PublicationStatus status,
            boolean archived) {
        return new LearningResource(resource.id(), resource.systemCode(), resource.notionCode(),
                resource.code(), resource.type(), resource.label(), resource.body(),
                resource.url(), resource.rank(), status, archived);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String identity(String systemCode, String code) {
        return systemCode + ":" + code;
    }

    private String normalize(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }
}
