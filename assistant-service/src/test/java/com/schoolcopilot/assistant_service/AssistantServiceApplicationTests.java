package com.schoolcopilot.assistant_service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.schoolcopilot.assistant_service.engine.AiEngine;
import com.schoolcopilot.assistant_service.repository.AssistantRepositories;
import com.schoolcopilot.assistant_service.service.AssistantService;
import com.schoolcopilot.assistant_service.web.AssistantController;

/**
 * Verifie le cablage complet : securite, moteur, trois clients HTTP, garde-fous.
 *
 * <p>La lecture des regles de securite se fait a la construction : ce test
 * attrape donc aussi un fichier JSON malforme, avant qu'il n'atteigne la
 * production.
 *
 * <p>Le service demarre <strong>sans</strong> ai-service : c'est tout l'interet
 * d'avoir commence par le port.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration"
})
class AssistantServiceApplicationTests {

    @MockitoBean
    AssistantRepositories.Conversations conversations;

    @MockitoBean
    AssistantRepositories.Messages messages;

    @MockitoBean
    AssistantRepositories.Quotas quotas;

    @Autowired
    AssistantService assistantService;

    @Autowired
    AssistantController assistantController;

    @Autowired
    AiEngine engine;

    @Test
    void contextLoads() {
        assertThat(assistantService).isNotNull();
        assertThat(assistantController).isNotNull();
    }

    @Test
    void theCannedEngineIsActiveByDefault() {
        // Tant qu'ai-service n'existe pas, le moteur bouchonne doit etre celui
        // qui repond — et il doit etre reconnaissable comme tel.
        assertThat(engine.name()).isEqualTo("canned");
    }
}
