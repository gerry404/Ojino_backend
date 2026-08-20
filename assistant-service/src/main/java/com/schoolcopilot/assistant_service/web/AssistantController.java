package com.schoolcopilot.assistant_service.web;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.assistant_service.service.AssistantService;
import com.schoolcopilot.assistant_service.service.QuotaPolicy;
import com.schoolcopilot.assistant_service.web.dto.AssistantDtos.AskRequest;
import com.schoolcopilot.assistant_service.web.dto.AssistantDtos.ConversationView;
import com.schoolcopilot.assistant_service.web.dto.AssistantDtos.ExchangeView;
import com.schoolcopilot.assistant_service.web.dto.AssistantDtos.FeedbackRequest;
import com.schoolcopilot.assistant_service.web.dto.AssistantDtos.MessageView;
import com.schoolcopilot.assistant_service.web.dto.AssistantDtos.NewConversationRequest;

import jakarta.validation.Valid;

/**
 * L'assistant de l'eleve connecte.
 *
 * <p>Toutes les routes travaillent sur le {@code sub} du token, et l'appartenance
 * de la conversation est verifiee : une conversation avec l'assistant est ce
 * qu'il y a de plus personnel dans ce produit, un identifiant devine ne doit rien
 * ouvrir.
 *
 * <p>Le jeton est retransmis aux services consultes, pour qu'ils repondent au nom
 * de l'eleve et non du service.
 */
@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {

    private final AssistantService assistant;

    public AssistantController(AssistantService assistant) {
        this.assistant = assistant;
    }

    @PostMapping("/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    public ConversationView create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody NewConversationRequest request) {

        return ConversationView.from(assistant.createConversation(jwt.getSubject(),
                request.systemCode(), request.notionCode(), request.title()));
    }

    @GetMapping("/conversations")
    public List<ConversationView> list(@AuthenticationPrincipal Jwt jwt) {
        return assistant.listConversations(jwt.getSubject()).stream()
                .map(ConversationView::from)
                .toList();
    }

    @GetMapping("/conversations/{id}")
    public List<MessageView> history(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String id) {
        return assistant.history(jwt.getSubject(), id).stream()
                .map(MessageView::from)
                .toList();
    }

    /**
     * Pose une question et renvoie la reponse.
     *
     * <p>Synchrone pour l'instant. Le streaming viendra avec le service temps
     * reel, en Go : le concevoir deux fois n'aurait pas de sens.
     */
    @PostMapping("/conversations/{id}/messages")
    public ExchangeView ask(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @Valid @RequestBody AskRequest request) {

        return ExchangeView.from(assistant.ask(jwt.getSubject(), id, request.question(),
                jwt.getTokenValue()));
    }

    @DeleteMapping("/conversations/{id}")
    public Map<String, String> delete(@AuthenticationPrincipal Jwt jwt,
            @PathVariable String id) {
        assistant.deleteConversation(jwt.getSubject(), id);
        return Map.of("status", "deleted");
    }

    /** Le retour de l'eleve : base de l'evaluation qualite. */
    @PostMapping("/messages/{id}/feedback")
    public MessageView rate(@AuthenticationPrincipal Jwt jwt, @PathVariable String id,
            @Valid @RequestBody FeedbackRequest request) {

        return MessageView.from(assistant.rate(jwt.getSubject(), id, request.feedback(),
                request.reason()));
    }

    /** Ce qu'il reste, pour le montrer avant que l'eleve ne bute dessus. */
    @GetMapping("/quota")
    public QuotaPolicy.Remaining quota(@AuthenticationPrincipal Jwt jwt) {
        return assistant.remainingQuota(jwt.getSubject());
    }
}
