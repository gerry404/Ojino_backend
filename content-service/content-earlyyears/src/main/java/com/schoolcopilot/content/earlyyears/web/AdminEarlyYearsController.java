package com.schoolcopilot.content.earlyyears.web;

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

import com.schoolcopilot.content.earlyyears.domain.LearningDomain;
import com.schoolcopilot.content.earlyyears.service.EarlyYearsService;

import jakarta.validation.Valid;

/**
 * Back-office des domaines d'apprentissage.
 *
 * <p>Protege par le prefixe {@code /api/v1/admin}, dont la regle est posee dans
 * {@code content-app} : le module de cycle n'a pas a connaitre la configuration
 * de securite de l'application qui l'heberge.
 */
@RestController
@RequestMapping("/api/v1/admin/reference/earlyyears")
public class AdminEarlyYearsController {

    private final EarlyYearsService earlyYears;

    public AdminEarlyYearsController(EarlyYearsService earlyYears) {
        this.earlyYears = earlyYears;
    }

    /** Inclut les domaines archives, que la route publique masque. */
    @GetMapping("/systems/{systemCode}/domains")
    public List<LearningDomain> list(@PathVariable String systemCode) {
        return earlyYears.listAll(systemCode);
    }

    @PostMapping("/systems/{systemCode}/domains")
    @ResponseStatus(HttpStatus.CREATED)
    public LearningDomain create(@PathVariable String systemCode,
            @Valid @RequestBody LearningDomainUpsertRequest request) {
        return earlyYears.create(systemCode, request.toDomain());
    }

    @PutMapping("/systems/{systemCode}/domains/{code}")
    public LearningDomain update(@PathVariable String systemCode, @PathVariable String code,
            @Valid @RequestBody LearningDomainUpsertRequest request) {
        return earlyYears.update(systemCode, code, request.toDomain());
    }

    /** Archive ou desarchive. Il n'existe pas de suppression. */
    @PostMapping("/systems/{systemCode}/domains/{code}/archived")
    public LearningDomain setArchived(@PathVariable String systemCode, @PathVariable String code,
            @RequestParam boolean value) {
        return earlyYears.setArchived(systemCode, code, value);
    }
}
