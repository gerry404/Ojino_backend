package com.schoolcopilot.user_service.domain.profile;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Les etapes de la creation de profil, dans l'ordre.
 *
 * <p>Le parcours est pilote par le serveur : le mobile et le web affichent la
 * meme sequence sans la coder chacun de leur cote.
 *
 * <p>Certaines etapes dependent du cycle. Un enfant de maternelle ne choisit ni
 * filiere ni matieres mais des domaines d'apprentissage ; un etudiant choisit un
 * parcours, un semestre et des unites d'enseignement. C'est
 * {@code content-service} qui dit lesquelles s'appliquent, et la reponse est
 * recopiee sur le profil au moment du choix de la classe.
 */
public enum OnboardingStep {

    /** Nom, prenom, date de naissance. */
    IDENTITY(1, true, false),

    /** Photo de profil : toujours passable, ce n'est pas un obstacle a l'entree. */
    PHOTO(2, false, false),

    /** Systeme scolaire et classe, suggeres a partir de l'age. */
    LEVEL(3, true, false),

    /** Filiere. College, lycee, prepa. */
    TRACK(4, true, true),

    /** Matieres suivies. College, lycee, prepa. */
    SUBJECTS(5, true, true),

    /** Domaines d'apprentissage. Maternelle et CP, a la place des matieres. */
    LEARNING_DOMAINS(6, true, true),

    /** Parcours universitaire : licence d'informatique, master de droit... */
    PROGRAM(7, true, true),

    /** Semestre en cours. */
    SEMESTER(8, true, true),

    /** Unites d'enseignement suivies. */
    COURSE_UNITS(9, true, true),

    /** Objectif poursuivi. */
    GOAL(10, true, false),

    /** Matieres ou domaines en difficulte. Une liste vide est une reponse valable. */
    DIFFICULTIES(11, true, false),

    /** Jours et creneaux de travail. */
    AVAILABILITY(12, true, false);

    private final int order;
    private final boolean required;
    private final boolean cycleDependent;

    OnboardingStep(int order, boolean required, boolean cycleDependent) {
        this.order = order;
        this.required = required;
        this.cycleDependent = cycleDependent;
    }

    public int order() {
        return order;
    }

    /** Une etape facultative peut etre sautee sans empecher la fin du parcours. */
    public boolean required() {
        return required;
    }

    /**
     * Vrai si cette etape ne concerne que certains cycles. Elle n'est alors
     * proposee que si {@code content-service} l'a annoncee pour la classe choisie.
     */
    public boolean cycleDependent() {
        return cycleDependent;
    }

    public static List<OnboardingStep> ordered() {
        return Arrays.stream(values())
                .sorted(Comparator.comparingInt(OnboardingStep::order))
                .toList();
    }
}
