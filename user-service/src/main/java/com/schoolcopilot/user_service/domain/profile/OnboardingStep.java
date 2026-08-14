package com.schoolcopilot.user_service.domain.profile;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Les etapes de la creation de profil, dans l'ordre.
 *
 * <p>Le parcours est pilote par le serveur : le mobile et le web affichent la
 * meme sequence sans la coder chacun de leur cote, et la faire evoluer ne demande
 * pas de mettre a jour les applications.
 *
 * <p>Chaque etape s'enregistre separement, ce qui rend le parcours reprenable :
 * quelqu'un qui ferme l'application au milieu retrouve exactement ou il s'etait
 * arrete.
 */
public enum OnboardingStep {

    /** Nom, prenom, date de naissance. */
    IDENTITY(1, true),

    /** Photo de profil : toujours passable, ce n'est pas un obstacle a l'entree. */
    PHOTO(2, false),

    /** Systeme scolaire et classe, suggeres a partir de l'age. */
    LEVEL(3, true),

    /** Filiere. Sautee automatiquement pour les niveaux qui n'en ont pas. */
    TRACK(4, true),

    /** Matieres suivies. */
    SUBJECTS(5, true),

    /** Objectif poursuivi. */
    GOAL(6, true),

    /** Matieres en difficulte. Une liste vide est une reponse valable. */
    DIFFICULTIES(7, true),

    /** Jours et creneaux de travail. */
    AVAILABILITY(8, true);

    private final int order;
    private final boolean required;

    OnboardingStep(int order, boolean required) {
        this.order = order;
        this.required = required;
    }

    public int order() {
        return order;
    }

    /** Une etape facultative peut etre sautee sans empecher la fin du parcours. */
    public boolean required() {
        return required;
    }

    public static List<OnboardingStep> ordered() {
        return Arrays.stream(values())
                .sorted(Comparator.comparingInt(OnboardingStep::order))
                .toList();
    }
}
