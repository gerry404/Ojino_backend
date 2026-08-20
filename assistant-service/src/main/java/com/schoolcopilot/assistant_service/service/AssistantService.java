package com.schoolcopilot.assistant_service.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.schoolcopilot.assistant_service.config.AssistantProperties;
import com.schoolcopilot.assistant_service.domain.Conversation;
import com.schoolcopilot.assistant_service.domain.LanguageRegister;
import com.schoolcopilot.assistant_service.domain.Message;
import com.schoolcopilot.assistant_service.domain.StudyContext;
import com.schoolcopilot.assistant_service.domain.UsageQuota;
import com.schoolcopilot.assistant_service.engine.AiEngine;
import com.schoolcopilot.assistant_service.engine.AiReply;
import com.schoolcopilot.assistant_service.engine.AiRequest;
import com.schoolcopilot.assistant_service.exception.ApiException;
import com.schoolcopilot.assistant_service.repository.AssistantRepositories;

/**
 * L'assistant : conversations, quotas, garde-fous, contexte.
 *
 * <p>Tout sauf l'inference, qui appartient au moteur. Ce service pourrait tourner
 * sans aucun modele derriere — et c'est exactement ce qu'il fait aujourd'hui.
 */
@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    private final AssistantRepositories.Conversations conversations;
    private final AssistantRepositories.Messages messages;
    private final AssistantRepositories.Quotas quotas;
    private final ContextBuilder contextBuilder;
    private final ContextWindow contextWindow;
    private final QuotaPolicy quotaPolicy;
    private final SafetyGuard safetyGuard;
    private final AiEngine engine;
    private final AssistantProperties properties;

    public AssistantService(AssistantRepositories.Conversations conversations,
            AssistantRepositories.Messages messages, AssistantRepositories.Quotas quotas,
            ContextBuilder contextBuilder, ContextWindow contextWindow,
            QuotaPolicy quotaPolicy, SafetyGuard safetyGuard, AiEngine engine,
            AssistantProperties properties) {
        this.conversations = conversations;
        this.messages = messages;
        this.quotas = quotas;
        this.contextBuilder = contextBuilder;
        this.contextWindow = contextWindow;
        this.quotaPolicy = quotaPolicy;
        this.safetyGuard = safetyGuard;
        this.engine = engine;
        this.properties = properties;
    }

    /** La question et sa reponse, telles qu'elles viennent d'etre enregistrees. */
    public record Exchange(Message question, Message answer) {
    }

    // ------------------------------------------------------------------
    // Conversations
    // ------------------------------------------------------------------

    public Conversation createConversation(String userId, String systemCode, String notionCode,
            String title) {

        Instant now = Instant.now();
        return conversations.save(new Conversation(null, userId,
                title == null || title.isBlank() ? "Nouvelle conversation" : title.trim(),
                systemCode, notionCode, 0, now, now, null));
    }

    public List<Conversation> listConversations(String userId) {
        return conversations.findByUserIdAndArchivedAtIsNullOrderByUpdatedAtDesc(userId);
    }

    public List<Message> history(String userId, String conversationId) {
        requireOwned(userId, conversationId);
        return messages.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    public void deleteConversation(String userId, String conversationId) {
        Conversation conversation = requireOwned(userId, conversationId);

        // Les messages partent avec le fil : les laisser derriere ferait
        // subsister le contenu le plus personnel du produit, sans plus rien pour
        // le retrouver ni le supprimer.
        messages.deleteByConversationId(conversationId);
        conversations.delete(conversation);
    }

    // ------------------------------------------------------------------
    // Poser une question
    // ------------------------------------------------------------------

    /**
     * Le parcours complet d'une question.
     *
     * <p>L'ordre est ce qui compte ici : garde-fous et quotas passent
     * <strong>avant</strong> l'appel au moteur. Verifier apres reviendrait a payer
     * la requete que l'on refuse.
     */
    public Exchange ask(String userId, String conversationId, String question,
            String bearerToken) {

        if (question == null || question.isBlank()) {
            throw ApiException.emptyQuestion();
        }

        Conversation conversation = requireOwned(userId, conversationId);
        String trimmed = question.trim();

        // 1. Les garde-fous d'abord : gratuits, et ils peuvent dispenser
        //    entierement d'appeler le moteur.
        SafetyGuard.Decision safety = safetyGuard.inspect(trimmed, "fr");

        if (safety instanceof SafetyGuard.Decision.Intercept intercept) {
            // La detresse ne passe pas par le modele. La reponse est ecrite a
            // l'avance et relue.
            return persist(conversation, userId, trimmed,
                    new AiReply(intercept.reply(), 0, 0, "safety-guard", List.of()));
        }

        // 2. Le contexte. Une panne du profil interrompt ici, avant tout cout.
        StudyContext context = contextBuilder.build(bearerToken, conversation.notionCode());
        LanguageRegister register = context.register();

        List<Message> recent = recentMessages(conversationId);
        AiRequest request = new AiRequest(contextWindow.build(recent), trimmed, register,
                context);

        // 3. Le quota, sur l'estimation : les jetons reels ne sont connus qu'apres
        //    la reponse, et attendre pour verifier reviendrait a la payer.
        UsageQuota usage = usageOf(userId);
        QuotaPolicy.Verdict verdict = quotaPolicy.evaluate(usage, trimmed.length(),
                request.estimatedInputTokens());

        if (verdict instanceof QuotaPolicy.Verdict.Denied denied) {
            throw ApiException.quotaExceeded(denied.code(), denied.reason());
        }

        // 4. Seulement maintenant, le moteur.
        AiReply reply = callEngine(request);
        String safeText = safetyGuard.enforceReplyLength(reply.text(), register);

        AiReply finalReply = new AiReply(safeText, reply.inputTokens(), reply.outputTokens(),
                reply.model(), reply.citedNotions());

        // 5. Le decompte reel, apres coup. Le leger depassement est accepte :
        //    refuser un depassement de deux pour cent frustrerait pour rien.
        quotas.save(usage.plus(1, finalReply.totalTokens()));

        return persist(conversation, userId, trimmed, finalReply);
    }

    // ------------------------------------------------------------------
    // Retour utilisateur
    // ------------------------------------------------------------------

    public Message rate(String userId, String messageId, Message.Feedback feedback,
            String reason) {

        Message message = messages.findById(messageId)
                .orElseThrow(() -> ApiException.messageNotFound(messageId));

        if (!message.userId().equals(userId)) {
            throw ApiException.notYours();
        }

        return messages.save(new Message(message.id(), message.conversationId(),
                message.userId(), message.role(), message.content(), message.inputTokens(),
                message.outputTokens(), message.model(), message.citedNotions(), feedback,
                reason, message.createdAt()));
    }

    // ------------------------------------------------------------------
    // Quota
    // ------------------------------------------------------------------

    public QuotaPolicy.Remaining remainingQuota(String userId) {
        return quotaPolicy.remainingFor(usageOf(userId));
    }

    // ------------------------------------------------------------------

    /**
     * Une panne du moteur ne doit pas laisser fuiter son message technique :
     * l'eleve n'en ferait rien, et il renseignerait un attaquant.
     */
    private AiReply callEngine(AiRequest request) {
        try {
            return engine.complete(request);
        } catch (AiEngine.EngineException e) {
            log.error("le moteur {} a echoue (reessayable : {}) : {}", engine.name(),
                    e.isRetryable(), e.getMessage());
            throw ApiException.engineUnavailable();
        }
    }

    /** Les derniers messages, remis dans l'ordre chronologique. */
    private List<Message> recentMessages(String conversationId) {
        List<Message> recent = new ArrayList<>(messages.findByConversationIdOrderByCreatedAtDesc(
                conversationId, PageRequest.of(0, properties.context().maxTurns())));
        Collections.reverse(recent);
        return recent;
    }

    private Exchange persist(Conversation conversation, String userId, String question,
            AiReply reply) {

        Instant now = Instant.now();

        Message asked = messages.save(new Message(null, conversation.id(), userId,
                Message.MessageRole.USER, question, 0, 0, null, List.of(), null, null, now));

        Message answered = messages.save(new Message(null, conversation.id(), userId,
                Message.MessageRole.ASSISTANT, reply.text(), reply.inputTokens(),
                reply.outputTokens(), reply.model(), reply.citedNotions(), null, null,
                now.plusMillis(1)));

        conversations.save(new Conversation(conversation.id(), conversation.userId(),
                titleFrom(conversation, question), conversation.systemCode(),
                conversation.notionCode(), conversation.messageCount() + 2,
                conversation.createdAt(), now, conversation.archivedAt()));

        return new Exchange(asked, answered);
    }

    /**
     * La premiere question donne son titre au fil.
     *
     * <p>Demander a l'eleve de nommer sa conversation avant de poser sa question
     * serait une friction inutile.
     */
    private String titleFrom(Conversation conversation, String question) {
        if (conversation.messageCount() > 0) {
            return conversation.title();
        }
        String candidate = question.length() <= 60 ? question : question.substring(0, 57) + "...";
        return candidate.replaceAll("\\s+", " ").trim();
    }

    private UsageQuota usageOf(String userId) {
        LocalDate today = LocalDate.now();
        return quotas.findById(UsageQuota.idFor(userId, today))
                .orElseGet(() -> UsageQuota.empty(userId, today));
    }

    private Conversation requireOwned(String userId, String conversationId) {
        Conversation conversation = conversations.findById(conversationId)
                .orElseThrow(() -> ApiException.conversationNotFound(conversationId));

        if (!conversation.belongsTo(userId)) {
            throw ApiException.notYours();
        }
        return conversation;
    }

    /** Identifiant lisible pour les journaux, sans exposer le contenu. */
    static String traceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
