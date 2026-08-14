package com.schoolcopilot.user_service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.schoolcopilot.user_service.repository.ReferenceRepositories;
import com.schoolcopilot.user_service.repository.StudentProfileRepository;
import com.schoolcopilot.user_service.service.profile.ProfileService;
import com.schoolcopilot.user_service.web.OnboardingController;
import com.schoolcopilot.user_service.web.ReferenceController;

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
class UserServiceApplicationTests {

    @MockitoBean
    StudentProfileRepository studentProfileRepository;

    @MockitoBean
    ReferenceRepositories.EducationSystems educationSystems;

    @MockitoBean
    ReferenceRepositories.EducationLevels educationLevels;

    @MockitoBean
    ReferenceRepositories.Tracks tracks;

    @MockitoBean
    ReferenceRepositories.Subjects subjects;

    @Autowired
    ProfileService profileService;

    @Autowired
    OnboardingController onboardingController;

    @Autowired
    ReferenceController referenceController;

    @Test
    void contextLoads() {
        assertThat(profileService).isNotNull();
        assertThat(onboardingController).isNotNull();
        assertThat(referenceController).isNotNull();
    }
}
