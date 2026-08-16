package com.schoolcopilot.assistant_service.web.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.schoolcopilot.assistant_service.domain.Conversation;
import com.schoolcopilot.assistant_service.domain.Message;
import com.schoolcopilot.assistant_service.service.AssistantService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Les objets d'entree et de sortie, regroupes pour rester lisibles. */
public final class AssistantDtos {

    private AssistantDtos() {
    }

    /**
     * @param notionCode ancre la conversation sur une notion, ce qui permet a
     *        l'assistant de s'appuyer sur ses prerequis
     */
    public record NewConversationRequest(
            @NotBlank(message = "Le systeme scolaire est obligatoire.")
            String systemCode,
            String notionCode,
            @Size(max = 120, message = "Le titre est trop long.")
            String title) {
    }

    /**
     * Une question.
     *
     * <p>La borne haute est genereuse : la vraie limite est celle des quotas, qui
     * rend un message d'erreur explicite. Celle-ci n'est qu'un garde-fou contre
     * l'envoi d'un fichier entier.
     */
    public record AskRequest(
            @NotBlank(message = "La question est obligatoire.")
            @Size(max = 10000, message = "La question est beaucoup trop longue.")
            String question) {
    }

    public record FeedbackRequest(
            @NotNull(message = "Le retour est obligatoire.")
            Message.Feedback feedback,
            @Size(max = 500, message = "La precision est trop longue.")
            String reason) {
    }

    /** Une conversation telle que les applications la voient. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ConversationView(
            String id,
            String title,
            String notionCode,
            int messageCount,
            Instant createdAt,
            Instant updatedAt) {

        public static ConversationView from(Conversation conversation) {
            return new ConversationView(conversation.id(), conversation.title(),
                    conversation.notionCode(), conversation.messageCount(),
                    conversation.createdAt(), conversation.updatedAt());
        }
    }

    /**
     * Un message tel que les applications le voient.
     *
     * <p>Ni le decompte de jetons, ni le modele : ce sont des rouages internes.
     * Les exposer inviterait a construire dessus, et ils changeront.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MessageView(
            String id,
            Message.MessageRole role,
            String content,
            List<String> citedNotions,
            Message.Feedback feedback,
            Instant createdAt) {

        public static MessageView from(Message message) {
            return new MessageView(message.id(), message.role(), message.content(),
                    message.citedNotions(), message.feedback(), message.createdAt());
        }
    }

    /** Une question et sa reponse, renvoyees ensemble. */
    public record ExchangeView(MessageView question, MessageView answer) {

        public static ExchangeView from(AssistantService.Exchange exchange) {
            return new ExchangeView(MessageView.from(exchange.question()),
                    MessageView.from(exchange.answer()));
        }
    }
}
