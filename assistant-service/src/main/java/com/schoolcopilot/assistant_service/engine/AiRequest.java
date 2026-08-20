package com.schoolcopilot.assistant_service.engine;

import java.util.List;

import com.schoolcopilot.assistant_service.domain.LanguageRegister;
import com.schoolcopilot.assistant_service.domain.StudyContext;

/**
 * Ce qu'on demande au moteur.
 *
 * @param history les echanges precedents, deja tronques a la fenetre utile
 * @param question la question posee maintenant
 * @param register le registre de langage attendu
 * @param context ce que l'on sait de l'eleve et du programme
 */
public record AiRequest(
        List<Turn> history,
        String question,
        LanguageRegister register,
        StudyContext context) {

    /** Un tour de conversation. */
    public record Turn(String role, String content) {

        public static final String USER = "user";
        public static final String ASSISTANT = "assistant";
    }

    /**
     * Estimation du cout avant l'appel.
     *
     * <p>Grossiere volontairement — environ quatre caracteres par jeton pour du
     * francais. Elle ne sert qu'a refuser ce qui depasse manifestement, avant de
     * payer la requete. Le decompte reel vient du moteur.
     */
    public int estimatedInputTokens() {
        int chars = question.length();
        for (Turn turn : history) {
            chars += turn.content().length();
        }
        chars += context == null ? 0 : context.approximateSize();
        return chars / 4;
    }
}
