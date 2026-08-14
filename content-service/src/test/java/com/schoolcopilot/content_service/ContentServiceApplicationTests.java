package com.schoolcopilot.content_service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.schoolcopilot.content_service.repository.ReferenceRepositories;
import com.schoolcopilot.content_service.service.reference.AdminReferenceService;
import com.schoolcopilot.content_service.service.reference.ReferenceService;
import com.schoolcopilot.content_service.web.admin.AdminReferenceController;
import com.schoolcopilot.content_service.web.client.ReferenceController;

/**
 * Verifie le cablage complet : securite, validation des tokens, controleurs.
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

    @Autowired
    ReferenceService referenceService;

    @Autowired
    AdminReferenceService adminReferenceService;

    @Autowired
    ReferenceController referenceController;

    @Autowired
    AdminReferenceController adminReferenceController;

    @Test
    void contextLoads() {
        assertThat(referenceService).isNotNull();
        assertThat(adminReferenceService).isNotNull();
        assertThat(referenceController).isNotNull();
        assertThat(adminReferenceController).isNotNull();
    }
}
