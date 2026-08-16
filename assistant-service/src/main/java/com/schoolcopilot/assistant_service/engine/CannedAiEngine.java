package com.schoolcopilot.assistant_service.engine;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Moteur de developpement : il fabrique une reponse plutot que d'en generer une.
 *
 * <p>Comme {@code LoggingSmsSender} dans l'auth-service, il permet d'eprouver
 * tout le parcours — quotas, garde-fous, contexte, historique — sans compte
 * aupres d'un fournisseur ni facture.
 *
 * <p>Deux exigences le gouvernent :
 * <ul>
 *   <li>etre reconnaissable au premier coup d'oeil, pour que personne ne prenne
 *       sa sortie pour une vraie reponse ;</li>
 *   <li>consommer des jetons plausibles, sans quoi les quotas ne pourraient pas
 *       etre eprouves.</li>
 * </ul>
 */
public class CannedAiEngine implements AiEngine {

    private static final Logger log = LoggerFactory.getLogger(CannedAiEngine.class);
    private static final String PREFIX = "[REPONSE SIMULEE] ";

    @Override
    public String name() {
        return "canned";
    }

    @Override
    public AiReply complete(AiRequest request) {
        log.warn("[MOTEUR SIMULE] question de {} caracteres, registre {}",
                request.question().length(), request.register());

        StringBuilder text = new StringBuilder(PREFIX);
        text.append("Tu demandes : \"").append(trim(request.question())).append("\". ");

        if (request.context() != null && request.context().notionLabel() != null) {
            text.append("Cela porte sur ").append(request.context().notionLabel()).append(". ");
        }
        if (request.context() != null && !request.context().prerequisites().isEmpty()) {
            text.append("Il faut d'abord maitriser ")
                    .append(String.join(", ", request.context().prerequisites()))
                    .append(". ");
        }
        text.append("Registre attendu : ").append(request.register()).append(".");

        String reply = text.toString();

        // Meme approximation que l'estimation d'entree : environ quatre
        // caracteres par jeton.
        return new AiReply(reply, request.estimatedInputTokens(), reply.length() / 4,
                "canned-v1", citedNotions(request));
    }

    private List<String> citedNotions(AiRequest request) {
        if (request.context() == null || request.context().notionCode() == null) {
            return List.of();
        }
        return List.of(request.context().notionCode());
    }

    private String trim(String question) {
        return question.length() <= 120 ? question : question.substring(0, 120) + "...";
    }
}
