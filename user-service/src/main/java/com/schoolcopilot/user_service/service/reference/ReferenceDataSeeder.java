package com.schoolcopilot.user_service.service.reference;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.schoolcopilot.user_service.config.ReferenceProperties;
import com.schoolcopilot.user_service.domain.reference.EducationLevel;
import com.schoolcopilot.user_service.domain.reference.EducationSystem;
import com.schoolcopilot.user_service.domain.reference.Subject;
import com.schoolcopilot.user_service.domain.reference.Track;
import com.schoolcopilot.user_service.repository.ReferenceRepositories;

import tools.jackson.databind.ObjectMapper;

/**
 * Charge les systemes scolaires livres avec l'application, au premier demarrage.
 *
 * <p>L'operation est volontairement conservatrice : un systeme deja present en
 * base est laisse intact. Les corrections faites directement dans Mongo, ou les
 * pays ajoutes en production, ne sont donc jamais ecrases par un redeploiement.
 */
@Component
public class ReferenceDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ReferenceDataSeeder.class);
    private static final String SEED_FILE = "reference/education-systems.json";

    private final ReferenceRepositories.EducationSystems systems;
    private final ReferenceRepositories.EducationLevels levels;
    private final ReferenceRepositories.Tracks tracks;
    private final ReferenceRepositories.Subjects subjects;
    private final ReferenceProperties properties;
    private final ObjectMapper objectMapper;

    public ReferenceDataSeeder(ReferenceRepositories.EducationSystems systems,
            ReferenceRepositories.EducationLevels levels,
            ReferenceRepositories.Tracks tracks,
            ReferenceRepositories.Subjects subjects,
            ReferenceProperties properties,
            ObjectMapper objectMapper) {
        this.systems = systems;
        this.levels = levels;
        this.tracks = tracks;
        this.subjects = subjects;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (!properties.seedOnStartup()) {
            return;
        }
        ReferenceSeed seed = read();
        seed.systems().forEach(this::seedSystem);
    }

    private ReferenceSeed read() throws IOException {
        try (InputStream input = new ClassPathResource(SEED_FILE).getInputStream()) {
            return objectMapper.readValue(input, ReferenceSeed.class);
        }
    }

    private void seedSystem(ReferenceSeed.SystemSeed seed) {
        if (systems.existsById(seed.code())) {
            log.debug("Systeme {} deja present, chargement ignore.", seed.code());
            return;
        }

        systems.save(new EducationSystem(seed.code(), seed.country(), seed.countryLabel(),
                seed.label(), seed.language(), seed.displayOrder(), true));

        levels.saveAll(seed.levels().stream()
                .map(level -> new EducationLevel(
                        id(seed.code(), level.code()), seed.code(), level.code(), level.label(),
                        level.cycle(), level.rank(), level.typicalAgeMin(), level.typicalAgeMax(),
                        level.hasTracks()))
                .toList());

        tracks.saveAll(seed.tracks().stream()
                .map(track -> new Track(
                        id(seed.code(), track.code()), seed.code(), track.code(), track.label(),
                        track.description(), track.levelCodes(), track.displayOrder()))
                .toList());

        subjects.saveAll(seed.subjects().stream()
                .map(subject -> new Subject(
                        id(seed.code(), subject.code()), seed.code(), subject.code(),
                        subject.label(), subject.levelCodes(), subject.trackCodes(),
                        subject.core(), subject.displayOrder()))
                .toList());

        log.info("Systeme scolaire {} charge : {} niveaux, {} filieres, {} matieres.",
                seed.code(), seed.levels().size(), seed.tracks().size(), seed.subjects().size());
    }

    /** Identifiant deterministe : recharger deux fois ne cree jamais de doublon. */
    private String id(String systemCode, String code) {
        return systemCode + ":" + code;
    }
}
