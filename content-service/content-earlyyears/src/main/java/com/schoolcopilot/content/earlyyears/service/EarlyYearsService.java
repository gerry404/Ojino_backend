package com.schoolcopilot.content.earlyyears.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.schoolcopilot.content.core.exception.ApiException;
import com.schoolcopilot.content.earlyyears.domain.LearningDomain;
import com.schoolcopilot.content.earlyyears.repository.LearningDomainRepository;

/**
 * Les domaines d'apprentissage de la maternelle et du CP.
 *
 * <p>Meme regle que dans le reste du referentiel : rien ne se supprime, tout
 * s'archive. Un domaine archive disparait des choix mais reste resolvable, donc
 * un profil qui le reference continue de fonctionner.
 */
@Service
public class EarlyYearsService {

    private static final Logger log = LoggerFactory.getLogger(EarlyYearsService.class);

    private final LearningDomainRepository domains;

    public EarlyYearsService(LearningDomainRepository domains) {
        this.domains = domains;
    }

    /** Les domaines proposes pour cette classe. */
    public List<LearningDomain> domainsFor(String systemCode, String levelCode) {
        return domains.findBySystemCodeOrderByDisplayOrderAsc(systemCode).stream()
                .filter(domain -> !domain.archived())
                .filter(domain -> domain.appliesTo(levelCode))
                .toList();
    }

    /** Inclut les domaines archives : le back-office doit pouvoir les desarchiver. */
    public List<LearningDomain> listAll(String systemCode) {
        return domains.findBySystemCodeOrderByDisplayOrderAsc(systemCode);
    }

    /** Verifie une selection et la debarrasse de ses doublons. */
    public List<String> validate(String systemCode, String levelCode, List<String> chosen) {
        Set<String> allowed = domainsFor(systemCode, levelCode).stream()
                .map(LearningDomain::code)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        Set<String> unknown = new LinkedHashSet<>(chosen);
        unknown.removeAll(allowed);
        if (!unknown.isEmpty()) {
            throw ApiException.unknownLearningDomains(unknown);
        }
        return List.copyOf(new LinkedHashSet<>(chosen));
    }

    public LearningDomain create(String systemCode, LearningDomain domain) {
        String code = normalize(domain.code());
        domains.findBySystemCodeAndCode(systemCode, code).ifPresent(existing -> {
            throw ApiException.alreadyExists("Le domaine", code);
        });
        return domains.save(new LearningDomain(systemCode + ":" + code, systemCode, code,
                domain.label(), domain.description(), domain.levelCodes(), domain.displayOrder(),
                false));
    }

    public LearningDomain update(String systemCode, String code, LearningDomain changes) {
        LearningDomain existing = require(systemCode, code);
        return domains.save(new LearningDomain(existing.id(), systemCode, code, changes.label(),
                changes.description(), changes.levelCodes(), changes.displayOrder(),
                existing.archived()));
    }

    public LearningDomain setArchived(String systemCode, String code, boolean archived) {
        LearningDomain domain = require(systemCode, code);
        log.info("Domaine {} du systeme {} {}.", code, systemCode,
                archived ? "archive" : "desarchive");
        return domains.save(new LearningDomain(domain.id(), domain.systemCode(), domain.code(),
                domain.label(), domain.description(), domain.levelCodes(), domain.displayOrder(),
                archived));
    }

    private LearningDomain require(String systemCode, String code) {
        return domains.findBySystemCodeAndCode(systemCode, code)
                .orElseThrow(() -> ApiException.unknownLearningDomains(code));
    }

    private String normalize(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }
}
