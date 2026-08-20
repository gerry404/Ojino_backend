package com.schoolcopilot.assistant_service.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Le moteur reel : {@code ai-service}, en FastAPI.
 *
 * <p>Cette classe est tout ce que le port avait achete. Les quotas, les
 * garde-fous, la fenetre de contexte et l'historique ont ete ecrits et testes
 * avant qu'elle n'existe ; rien de tout cela ne change ici. Le basculement tient
 * en une propriete : {@code ojino.assistant.engine=remote}.
 *
 * <p><strong>Ce que cette classe ne fait pas.</strong> Elle ne verifie aucun
 * quota, ne filtre aucune question et ne construit aucun contexte : tout cela est
 * deja fait en amont par {@code AssistantService}. Elle traduit un appel Java en
 * appel HTTP, et rien d'autre. Y ajouter une regle metier la ferait diverger du
 * moteur bouchonne, et les deux cesseraient d'etre interchangeables — ce qui
 * viderait le port de son interet.
 */
public class RemoteAiEngine implements AiEngine {

    private static final Logger log = LoggerFactory.getLogger(RemoteAiEngine.class);

    private final RestClient restClient;

    public RemoteAiEngine(RestClient aiRestClient) {
        this.restClient = aiRestClient;
    }

    @Override
    public String name() {
        return "remote";
    }

    @Override
    public AiReply complete(AiRequest request) {
        try {
            AiReply reply = restClient.post()
                    .uri("/api/v1/completion")
                    .body(request)
                    .retrieve()
                    // Le traitement des erreurs est repris ici parce que le
                    // comportement par defaut leve une exception dont l'appelant
                    // ne peut rien tirer : il a besoin de savoir si reessayer a
                    // une chance d'aboutir.
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw translate(res.getStatusCode());
                    })
                    .body(AiReply.class);

            if (reply == null || reply.text() == null || reply.text().isBlank()) {
                // Un corps vide avec un statut 200 est le pire des cas : sans ce
                // controle, l'eleve recevrait une bulle vide et le quota serait
                // debite pour rien.
                throw new EngineException("ai-service a repondu un corps vide", true);
            }

            return reply;

        } catch (EngineException e) {
            throw e;

        } catch (RestClientException e) {
            // Delai depasse, connexion refusee, corps illisible. Toujours
            // reessayable : ce sont des pannes de transport, pas des refus.
            log.error("ai-service injoignable : {}", e.getMessage());
            throw new EngineException("ai-service injoignable", true);
        }
    }

    /**
     * Traduit un statut HTTP en echec typé.
     *
     * <p>Le corps de la reponse distante n'est jamais repris : il finirait dans
     * un message d'erreur rendu a l'eleve, et documenterait l'infrastructure pour
     * qui la sonde. Le statut suffit a decider, les journaux gardent le reste.
     */
    private EngineException translate(HttpStatusCode status) {
        if (status.value() == 401) {
            // Ni passager ni reessayable : le secret partage est faux des deux
            // cotes. Trace en erreur parce que c'est une panne de configuration,
            // et qu'elle passerait autrement pour une indisponibilite.
            log.error("ai-service refuse le jeton interne : verifier OJINO_INTERNAL_TOKEN "
                    + "des deux cotes");
            return new EngineException("authentification refusee par ai-service", false);
        }

        if (status.is4xxClientError()) {
            // La requete est refusee sur le fond. La reenvoyer telle quelle ne
            // ferait que repayer le meme echec.
            log.warn("ai-service a refuse la requete ({})", status.value());
            return new EngineException("requete refusee par ai-service", false);
        }

        // 5xx, y compris le 503 que rend ai-service quand son fournisseur est
        // surcharge.
        log.error("ai-service en echec ({})", status.value());
        return new EngineException("ai-service en echec", true);
    }
}
