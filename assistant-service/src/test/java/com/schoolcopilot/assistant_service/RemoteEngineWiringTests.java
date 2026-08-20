package com.schoolcopilot.assistant_service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.schoolcopilot.assistant_service.engine.AiEngine;
import com.schoolcopilot.assistant_service.repository.AssistantRepositories;

/**
 * Le basculement vers le moteur reel.
 *
 * <p>C'est le test qui vaut la classe : une implementation parfaite qu'aucune
 * configuration n'active ne sert a rien. Il verifie qu'une seule propriete
 * suffit — {@code ojino.assistant.engine=remote} — et que rien d'autre dans le
 * cablage n'a besoin de changer. C'est exactement ce que le port avait achete.
 */
@SpringBootTest(properties = {
        "ojino.assistant.engine=remote",
        "ojino.assistant.ai.base-url=http://localhost:8091",
        "ojino.assistant.ai.internal-token=jeton-interne-de-test",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration,"
                + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoRepositoriesAutoConfiguration"
})
class RemoteEngineWiringTests {

    @MockitoBean
    AssistantRepositories.Conversations conversations;

    @MockitoBean
    AssistantRepositories.Messages messages;

    @MockitoBean
    AssistantRepositories.Quotas quotas;

    @Autowired
    AiEngine engine;

    @Test
    void onePropertySwitchesTheEngine() {
        assertThat(engine.name()).isEqualTo("remote");
    }

    @Test
    void thereIsExactlyOneEngine(@Autowired org.springframework.context.ApplicationContext context) {
        // Les deux moteurs sont exclusifs par construction. Si les deux beans
        // coexistaient un jour, l'injection echouerait au demarrage — mais ce
        // test le dit plus clairement qu'une NoUniqueBeanDefinitionException.
        assertThat(context.getBeansOfType(AiEngine.class)).hasSize(1);
    }
}
