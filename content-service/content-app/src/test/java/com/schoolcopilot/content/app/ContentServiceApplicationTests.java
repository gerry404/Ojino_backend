package com.schoolcopilot.content.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.schoolcopilot.content.core.domain.EducationCycle;
import com.schoolcopilot.content.core.repository.CurriculumRepositories;
import com.schoolcopilot.content.core.repository.ReferenceRepositories;
import com.schoolcopilot.content.core.spi.CurriculumModules;
import com.schoolcopilot.content.core.web.client.ReferenceController;
import com.schoolcopilot.content.earlyyears.repository.LearningDomainRepository;
import com.schoolcopilot.content.earlyyears.web.EarlyYearsController;
import com.schoolcopilot.content.university.repository.UniversityRepositories;
import com.schoolcopilot.content.university.web.UniversityController;

/**
 * Verifie que l'application assemble bien le coeur et tous les modules de cycle.
 *
 * <p>C'est le test qui protege le decoupage : si un module cesse d'etre ramasse
 * par le scan, ou si son {@code CurriculumModule} disparait, il echoue ici plutot
 * qu'au premier appel en production.
 *
 * <p>Les autoconfigurations Mongo sont ecartees et les repositories remplaces par
 * des doublures, car {@code mongoTemplate} ouvre une connexion des sa creation.
 */
@SpringBootTest(properties = {
        "ojino.reference.seed-on-startup=false",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration"
})
class ContentServiceApplicationTests {

    @MockitoBean
    ReferenceRepositories.EducationSystems educationSystems;

    @MockitoBean
    ReferenceRepositories.EducationLevels educationLevels;

    @MockitoBean
    ReferenceRepositories.Tracks tracks;

    @MockitoBean
    ReferenceRepositories.Subjects subjects;

    @MockitoBean
    CurriculumRepositories.Chapters chapters;

    @MockitoBean
    CurriculumRepositories.Notions notions;

    @MockitoBean
    CurriculumRepositories.LearningResources learningResources;

    @MockitoBean
    CurriculumRepositories.Exercises exercises;

    @MockitoBean
    LearningDomainRepository learningDomains;

    @MockitoBean
    UniversityRepositories.Programs programs;

    @MockitoBean
    UniversityRepositories.CourseUnits courseUnits;

    @Autowired
    CurriculumModules curriculumModules;

    @Autowired
    ReferenceController referenceController;

    @Autowired
    EarlyYearsController earlyYearsController;

    @Autowired
    UniversityController universityController;

    @Test
    void contextLoads() {
        assertThat(referenceController).isNotNull();
        assertThat(earlyYearsController).isNotNull();
        assertThat(universityController).isNotNull();
    }

    @Test
    void everyCycleModuleIsRegistered() {
        // Les cinq modules Maven doivent se retrouver dans l'annuaire, chacun une
        // fois. Un module absent du pom disparaitrait silencieusement sans ce test.
        assertThat(curriculumModules.all())
                .extracting(module -> module.cycle())
                .containsExactly(
                        EducationCycle.EARLY_YEARS,
                        EducationCycle.COLLEGE,
                        EducationCycle.HIGH_SCHOOL,
                        EducationCycle.PREPA,
                        EducationCycle.UNIVERSITY);
    }
}
