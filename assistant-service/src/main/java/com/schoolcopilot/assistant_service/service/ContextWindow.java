package com.schoolcopilot.assistant_service.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.springframework.stereotype.Component;

import com.schoolcopilot.assistant_service.config.AssistantProperties;
import com.schoolcopilot.assistant_service.domain.Message;
import com.schoolcopilot.assistant_service.engine.AiRequest;

/**
 * Choisit quels messages du fil accompagnent la question.
 *
 * <p>On ne peut pas tout renvoyer : une conversation de quatre cents messages
 * couterait cher et depasserait la fenetre du modele. Il faut donc trancher, et
 * la strategie retenue est volontairement la plus simple — les N derniers tours.
 *
 * <p>Isole dans sa propre classe pour pouvoir en changer sans toucher au reste.
 * La suite logique serait un resume des anciens messages suivi des N derniers,
 * mais cela suppose un appel supplementaire au modele : a faire quand le besoin
 * sera mesure, pas avant.
 */
@Component
public class ContextWindow {

    private final AssistantProperties properties;

    public ContextWindow(AssistantProperties properties) {
        this.properties = properties;
    }

    /**
     * Construit l'historique a envoyer.
     *
     * <p>Deux limites se combinent : un nombre de tours et un volume de
     * caracteres. La premiere garde le fil coherent, la seconde protege du cas ou
     * quelques messages tres longs suffiraient a tout saturer.
     *
     * @param messages le fil complet, du plus ancien au plus recent
     */
    public List<AiRequest.Turn> build(List<Message> messages) {
        Deque<AiRequest.Turn> selected = new ArrayDeque<>();
        int budget = properties.context().maxHistoryChars();
        int turns = 0;

        // Parcours a rebours : ce sont les messages recents qui comptent, et ce
        // sont eux qu'il faut garder si le budget ne permet pas tout.
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (turns >= properties.context().maxTurns()) {
                break;
            }

            Message message = messages.get(i);
            if (message.content().length() > budget) {
                break;
            }

            selected.addFirst(new AiRequest.Turn(roleOf(message), message.content()));
            budget -= message.content().length();
            turns++;
        }

        return new ArrayList<>(selected);
    }

    private String roleOf(Message message) {
        return message.role() == Message.MessageRole.USER
                ? AiRequest.Turn.USER
                : AiRequest.Turn.ASSISTANT;
    }
}
