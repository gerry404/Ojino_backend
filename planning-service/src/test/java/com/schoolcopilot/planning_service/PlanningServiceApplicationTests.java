package com.schoolcopilot.planning_service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.schoolcopilot.planning_service.repository.PlanningRepositories;
import com.schoolcopilot.planning_service.service.PlanningService;
import com.schoolcopilot.planning_service.web.PlanningController;

/**
 * Verifie le cablage complet : securite, validation des tokens, les deux clients
 * HTTP, controleurs.
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
class PlanningServiceApplicationTests {

    @MockitoBean
    PlanningRepositories.Deadlines deadlines;

    @MockitoBean
    PlanningRepositories.Sessions sessions;

    @Autowired
    PlanningService planningService;

    @Autowired
    PlanningController planningController;

    @Test
    void contextLoads() {
        assertThat(planningService).isNotNull();
        assertThat(planningController).isNotNull();
    }
}
