package com.schoolcopilot.content.earlyyears.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.schoolcopilot.content.earlyyears.domain.LearningDomain;
import com.schoolcopilot.content.earlyyears.repository.LearningDomainRepository;

import tools.jackson.databind.ObjectMapper;

/**
 * Charge les domaines d'apprentissage livres avec le module.
 *
 * <p>Le module porte ses propres donnees et son propre chargement, sans rien
 * demander a {@code content-core}. C'est ce qui le rend extractible : le jour ou
 * la maternelle devient une application separee, ce fichier et ce chargeur
 * partent avec, entiers.
 *
 * <p>Conservateur comme celui du coeur : un systeme qui a deja des domaines n'est
 * jamais reecrase.
 */
@Component
public class EarlyYearsSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EarlyYearsSeeder.class);
    private static final String SEED_FILE = "reference/early-years-domains.json";

    private final LearningDomainRepository domains;
    private final ObjectMapper objectMapper;

    public EarlyYearsSeeder(LearningDomainRepository domains, ObjectMapper objectMapper) {
        this.domains = domains;
        this.objectMapper = objectMapper;
    }

    record Seed(List<SystemSeed> systems) {
    }

    record SystemSeed(String systemCode, List<DomainSeed> domains) {
    }

    record DomainSeed(String code, String label, String description, List<String> levelCodes,
            int displayOrder) {
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        Seed seed;
        try (InputStream input = new ClassPathResource(SEED_FILE).getInputStream()) {
            seed = objectMapper.readValue(input, Seed.class);
        }
        seed.systems().forEach(this::seedSystem);
    }

    private void seedSystem(SystemSeed seed) {
        if (domains.countBySystemCode(seed.systemCode()) > 0) {
            log.debug("Domaines deja presents pour {}, chargement ignore.", seed.systemCode());
            return;
        }

        domains.saveAll(seed.domains().stream()
                .map(domain -> new LearningDomain(
                        seed.systemCode() + ":" + domain.code(),
                        seed.systemCode(),
                        domain.code(),
                        domain.label(),
                        domain.description(),
                        domain.levelCodes(),
                        domain.displayOrder(),
                        false))
                .toList());

        log.info("{} domaines d'apprentissage charges pour {}.",
                seed.domains().size(), seed.systemCode());
    }
}
