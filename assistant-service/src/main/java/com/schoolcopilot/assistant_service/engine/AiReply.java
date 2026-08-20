package com.schoolcopilot.assistant_service.engine;

import java.util.List;

/**
 * Ce que le moteur repond.
 *
 * <p>Un record et non une simple chaine : le jour ou il faut facturer, deboguer
 * ou tracer, on aurait a reprendre tous les appelants. Le cout de la prevoyance
 * est ici de cinq minutes.
 *
 * @param inputTokens jetons reellement consommes en entree
 * @param outputTokens jetons produits
 * @param model identifiant du modele, pour la tracabilite : comprendre une
 *        mauvaise reponse six mois plus tard suppose de savoir qui l'a produite
 * @param citedNotions notions du programme sur lesquelles la reponse s'appuie
 */
public record AiReply(
        String text,
        int inputTokens,
        int outputTokens,
        String model,
        List<String> citedNotions) {

    public int totalTokens() {
        return inputTokens + outputTokens;
    }

    public List<String> citedNotions() {
        return citedNotions == null ? List.of() : citedNotions;
    }
}
