package com.schoolcopilot.assistant_service.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.schoolcopilot.assistant_service.domain.LanguageRegister;
import com.schoolcopilot.assistant_service.domain.StudyContext;
import com.schoolcopilot.assistant_service.engine.AiEngine.EngineException;

/**
 * Le moteur distant.
 *
 * <p>Ce qui est verifie ici n'est pas la qualite des reponses — elle ne se teste
 * pas de ce cote — mais le contrat : ce qui part sur le fil, ce qui en revient,
 * et la facon dont chaque panne est traduite. Un echec mal classe est le pire
 * des cas : il fait reessayer ce qui echouera toujours, ou abandonner ce qui
 * aurait abouti.
 */
class RemoteAiEngineTest {

    private static final String BASE_URL = "http://ai-service:8091";
    private static final String TOKEN = "jeton-interne-de-test";

    private MockRestServiceServer server;
    private RemoteAiEngine engine;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("X-Internal-Token", TOKEN);

        // Se lie au constructeur : le client obtenu est un vrai RestClient, seul
        // le transport est intercepte.
        this.server = MockRestServiceServer.bindTo(builder).build();
        this.engine = new RemoteAiEngine(builder.build());
    }

    private AiRequest request() {
        StudyContext context = new StudyContext("CM-FR", "4EME", "4eme", "COLLEGE", null,
                List.of("MATH"), List.of("FRACTIONS"), "PYTHAGORE",
                "Le theoreme de Pythagore", "Dans un triangle rectangle...",
                List.of("CARRES"), true);

        return new AiRequest(
                List.of(new AiRequest.Turn(AiRequest.Turn.USER, "bonjour")),
                "Comment calculer l'hypotenuse ?",
                LanguageRegister.COLLEGE,
                context);
    }

    private String reply() {
        return """
                {
                  "text": "L'hypotenuse est le plus grand cote.",
                  "inputTokens": 120,
                  "outputTokens": 40,
                  "model": "gpt-4o-mini",
                  "citedNotions": ["PYTHAGORE"]
                }
                """;
    }

    @Test
    void itSendsTheContractAiServiceExpects() {
        server.expect(requestTo(BASE_URL + "/api/v1/completion"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // camelCase des deux cotes. Une cle en snake_case arriverait a
                // null cote Python sans la moindre erreur — exactement le piege
                // rencontre sur orientationDetresse.
                .andExpect(jsonPath("$.question").value("Comment calculer l'hypotenuse ?"))
                .andExpect(jsonPath("$.register").value("COLLEGE"))
                .andExpect(jsonPath("$.history[0].role").value("user"))
                .andExpect(jsonPath("$.context.systemCode").value("CM-FR"))
                .andExpect(jsonPath("$.context.notionCode").value("PYTHAGORE"))
                .andExpect(jsonPath("$.context.strugglingNotions[0]").value("FRACTIONS"))
                .andExpect(jsonPath("$.context.contentAvailable").value(true))
                .andRespond(withSuccess(reply(), MediaType.APPLICATION_JSON));

        engine.complete(request());

        server.verify();
    }

    @Test
    void itCarriesTheInternalToken() {
        // ai-service n'est jamais expose publiquement : sans cet en-tete, tout
        // repond 401 et le service entier est muet.
        server.expect(header("X-Internal-Token", TOKEN))
                .andRespond(withSuccess(reply(), MediaType.APPLICATION_JSON));

        engine.complete(request());

        server.verify();
    }

    @Test
    void itReadsTheReplyIncludingTheTokenCounts() {
        server.expect(requestTo(BASE_URL + "/api/v1/completion"))
                .andRespond(withSuccess(reply(), MediaType.APPLICATION_JSON));

        AiReply reply = engine.complete(request());

        assertThat(reply.text()).isEqualTo("L'hypotenuse est le plus grand cote.");
        assertThat(reply.model()).isEqualTo("gpt-4o-mini");
        assertThat(reply.citedNotions()).containsExactly("PYTHAGORE");
        // Sans ces deux valeurs, les quotas ne peuvent ni limiter ni facturer.
        assertThat(reply.inputTokens()).isEqualTo(120);
        assertThat(reply.outputTokens()).isEqualTo(40);
        assertThat(reply.totalTokens()).isEqualTo(160);
    }

    @Test
    void aServerErrorIsRetryable() {
        server.expect(requestTo(BASE_URL + "/api/v1/completion"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> engine.complete(request()))
                .isInstanceOf(EngineException.class)
                .extracting(e -> ((EngineException) e).isRetryable())
                .isEqualTo(true);
    }

    @Test
    void anOverloadedProviderIsRetryable() {
        // 503 est ce que rend ai-service quand son fournisseur est surcharge :
        // c'est le cas ou reessayer a le plus de chances d'aboutir.
        server.expect(requestTo(BASE_URL + "/api/v1/completion"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> engine.complete(request()))
                .isInstanceOf(EngineException.class)
                .extracting(e -> ((EngineException) e).isRetryable())
                .isEqualTo(true);
    }

    @Test
    void aRejectedRequestIsNotRetryable() {
        // Une question refusee sur le fond le restera : la reenvoyer ne ferait
        // que repayer le meme echec.
        server.expect(requestTo(BASE_URL + "/api/v1/completion"))
                .andRespond(withStatus(HttpStatus.PAYLOAD_TOO_LARGE));

        assertThatThrownBy(() -> engine.complete(request()))
                .isInstanceOf(EngineException.class)
                .extracting(e -> ((EngineException) e).isRetryable())
                .isEqualTo(false);
    }

    @Test
    void aRefusedTokenIsNotRetryable() {
        // Panne de configuration, pas d'indisponibilite. Reessayer ne corrigera
        // jamais un secret different des deux cotes.
        server.expect(requestTo(BASE_URL + "/api/v1/completion"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> engine.complete(request()))
                .isInstanceOf(EngineException.class)
                .extracting(e -> ((EngineException) e).isRetryable())
                .isEqualTo(false);
    }

    @Test
    void anEmptyBodyIsAFailureNotAnEmptyAnswer() {
        // Le pire des cas : un 200 avec un corps vide. Sans ce controle l'eleve
        // recevrait une bulle vide, et son quota serait debite pour rien.
        String blank = "{\"text\": \"  \", \"inputTokens\": 10, \"outputTokens\": 0,"
                + " \"model\": \"gpt-4o-mini\", \"citedNotions\": []}";

        server.expect(requestTo(BASE_URL + "/api/v1/completion"))
                .andRespond(withSuccess(blank, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> engine.complete(request()))
                .isInstanceOf(EngineException.class);
    }

    @Test
    void theRemoteDetailNeverReachesTheCaller() {
        server.expect(requestTo(BASE_URL + "/api/v1/completion"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Traceback: fichier /opt/ai/providers/openai.py ligne 42, "
                                + "cle sk-proj-secrete")
                        .contentType(MediaType.TEXT_PLAIN));

        // Le corps distant finirait dans un message rendu a l'eleve et
        // documenterait l'infrastructure pour qui la sonde.
        assertThatThrownBy(() -> engine.complete(request()))
                .isInstanceOf(EngineException.class)
                .hasMessageNotContaining("sk-proj-secrete")
                .hasMessageNotContaining("Traceback");
    }

    @Test
    void itIsNamedForTraceability() {
        // Le nom part dans les journaux et les messages stockes : savoir six mois
        // plus tard si une reponse vient d'un vrai modele ou du bouchon change
        // tout dans un diagnostic.
        assertThat(engine.name()).isEqualTo("remote");
    }
}
