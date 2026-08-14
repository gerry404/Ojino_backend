package com.schoolcopilot.learning_service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.schoolcopilot.learning_service.repository.LearningRepositories;
import com.schoolcopilot.learning_service.service.LearningService;
import com.schoolcopilot.learning_service.web.LearningController;

/**
 * Verifie le cablage complet : securite, validation des tokens, client HTTP vers
 * content-service, controleurs.
 *
 * <p>Les autoconfigurations Mongo sont ecartees et les repositories remplaces par
 * des doublures, car {@code mongoTemplate} ouvre une connexion des sa creation.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration"
})
class LearningServiceApplicationTests {

    @MockitoBean
    LearningRepositories.Events events;

    @MockitoBean
    LearningRepositories.Mastery mastery;

    @Autowired
    LearningService learningService;

    @Autowired
    LearningController learningController;

    @Test
    void contextLoads() {
        assertThat(learningService).isNotNull();
        assertThat(learningController).isNotNull();
    }
}
