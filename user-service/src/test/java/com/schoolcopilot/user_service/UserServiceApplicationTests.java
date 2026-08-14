package com.schoolcopilot.user_service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.schoolcopilot.user_service.repository.StudentProfileRepository;
import com.schoolcopilot.user_service.service.profile.ProfileService;
import com.schoolcopilot.user_service.web.client.OnboardingController;
import com.schoolcopilot.user_service.web.client.ProfileController;

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

    /** Le back-office l'utilise pour ses recherches a filtres facultatifs. */
    @MockitoBean
    MongoTemplate mongoTemplate;

    @Autowired
    ProfileService profileService;

    @Autowired
    OnboardingController onboardingController;

    @Autowired
    ProfileController profileController;

    @Test
    void contextLoads() {
        assertThat(profileService).isNotNull();
        assertThat(onboardingController).isNotNull();
        assertThat(profileController).isNotNull();
    }
}
