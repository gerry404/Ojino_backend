package com.schoolcopilot.assistant_service.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schoolcopilot.assistant_service.config.AssistantProperties;
import com.schoolcopilot.assistant_service.domain.Message;
import com.schoolcopilot.assistant_service.engine.AiRequest;

/**
 * La fenetre de contexte : que renvoyer au modele d'une conversation qui peut
 * compter des centaines de messages.
 *
 * <p>Reglages : 4 tours au plus, 100 caracteres d'historique.
 */
class ContextWindowTest {

    private final ContextWindow window = new ContextWindow(new AssistantProperties("canned",
            new AssistantProperties.Quota(50, 100000, 4000),
            new AssistantProperties.Context(4, 100)));

    @Test
    @DisplayName("une conversation courte passe entierement")
    void shortConversationPassesWhole() {
        assertThat(window.build(conversation(3))).hasSize(3);
    }

    @Test
    @DisplayName("le nombre de tours est plafonne")
    void turnCountIsCapped() {
        assertThat(window.build(conversation(20))).hasSize(4);
    }

    @Test
    @DisplayName("ce sont les messages recents qui sont gardes")
    void recentMessagesAreKept() {
        List<AiRequest.Turn> turns = window.build(conversation(20));

        // Le contexte utile est celui de la fin du fil, pas de son debut.
        assertThat(turns.getLast().content()).contains("message-19");
    }

    @Test
    @DisplayName("l'ordre chronologique est conserve")
    void chronologicalOrderIsPreserved() {
        List<AiRequest.Turn> turns = window.build(conversation(20));

        // Le parcours se fait a rebours, mais le resultat doit se lire dans le
        // sens de la conversation.
        assertThat(turns.get(0).content()).contains("message-16");
        assertThat(turns.get(1).content()).contains("message-17");
    }

    @Test
    @DisplayName("le budget de caracteres coupe avant le nombre de tours")
    void charBudgetCutsBeforeTurnCount() {
        List<Message> longOnes = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            longOnes.add(message(i, "x".repeat(60)));
        }

        // Quatre messages de soixante caracteres : le budget de cent n'en laisse
        // passer qu'un seul. Sans cette limite, quelques messages tres longs
        // suffiraient a tout saturer.
        assertThat(window.build(longOnes)).hasSize(1);
    }

    @Test
    @DisplayName("un fil vide donne un contexte vide")
    void emptyConversationGivesEmptyContext() {
        assertThat(window.build(List.of())).isEmpty();
    }

    @Test
    @DisplayName("les roles sont traduits pour le moteur")
    void rolesAreMapped() {
        List<AiRequest.Turn> turns = window.build(conversation(2));

        assertThat(turns.get(0).role()).isEqualTo(AiRequest.Turn.USER);
        assertThat(turns.get(1).role()).isEqualTo(AiRequest.Turn.ASSISTANT);
    }

    private List<Message> conversation(int count) {
        List<Message> messages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            messages.add(message(i, "message-" + i));
        }
        return messages;
    }

    private Message message(int index, String content) {
        Message.MessageRole role = index % 2 == 0
                ? Message.MessageRole.USER
                : Message.MessageRole.ASSISTANT;

        return new Message("m" + index, "conv-1", "user-1", role, content, 0, 0, null,
                List.of(), null, null, Instant.now().plusSeconds(index));
    }
}
