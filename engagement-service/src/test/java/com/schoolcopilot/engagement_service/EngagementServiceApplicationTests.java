package com.schoolcopilot.engagement_service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.schoolcopilot.engagement_service.domain.Badge;
import com.schoolcopilot.engagement_service.repository.EngagementRepositories;
import com.schoolcopilot.engagement_service.service.EngagementService;
import com.schoolcopilot.engagement_service.web.EngagementController;
import com.schoolcopilot.engagement_service.web.InternalEngagementController;

/**
 * Verifie le cablage complet : securite, filtre interne, client de notification,
 * controleurs.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration"
})
class EngagementServiceApplicationTests {

    @MockitoBean
    EngagementRepositories.Streaks streaks;

    @MockitoBean
    EngagementRepositories.Profiles profiles;

    @MockitoBean
    EngagementRepositories.CheckIns checkIns;

    @Autowired
    EngagementService engagementService;

    @Autowired
    EngagementController engagementController;

    @Autowired
    InternalEngagementController internalController;

    @Test
    void contextLoads() {
        assertThat(engagementService).isNotNull();
        assertThat(engagementController).isNotNull();
        assertThat(internalController).isNotNull();
    }

    @Test
    void everyBadgeHasAReachableThreshold() {
        // Un badge dont le seuil vaut zero serait obtenu avant toute activite, et
        // un seuil negatif n'a pas de sens.
        for (Badge badge : Badge.values()) {
            assertThat(badge.threshold()).as("seuil de %s", badge).isPositive();
            assertThat(badge.label()).as("libelle de %s", badge).isNotBlank();
        }
    }
}
