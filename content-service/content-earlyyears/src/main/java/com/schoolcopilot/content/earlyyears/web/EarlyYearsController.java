package com.schoolcopilot.content.earlyyears.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.content.earlyyears.service.EarlyYearsService;

/**
 * Le referentiel propre a la maternelle et au CP.
 *
 * <p>Son prefixe le distingue du referentiel commun : le jour ou ce module
 * devient une application separee, ce sont ces routes-la qui demenagent, et
 * seulement elles.
 */
@RestController
@RequestMapping("/api/v1/reference/earlyyears")
public class EarlyYearsController {

    private final EarlyYearsService earlyYears;

    public EarlyYearsController(EarlyYearsService earlyYears) {
        this.earlyYears = earlyYears;
    }

    /** Les domaines d'apprentissage de cette classe. */
    @GetMapping("/systems/{systemCode}/levels/{levelCode}/domains")
    public List<LearningDomainView> domains(@PathVariable String systemCode,
            @PathVariable String levelCode) {
        return earlyYears.domainsFor(systemCode, levelCode).stream()
                .map(LearningDomainView::from)
                .toList();
    }
}
