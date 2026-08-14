package com.schoolcopilot.learning_service.domain;

/** La nature d'un fait d'apprentissage. */
public enum LearningEventType {

    /** A consulte une lecon ou une fiche. Aucun resultat. */
    RESOURCE_VIEWED(false),

    /** A rendu un exercice. */
    EXERCISE_ATTEMPTED(true),

    /** A repondu a une question de quiz. */
    QUIZ_ANSWERED(true),

    /** A passe une evaluation complete. */
    ASSESSMENT_COMPLETED(true),

    /** S'est declare a l'aise ou en difficulte, de lui-meme. */
    SELF_ASSESSED(true);

    private final boolean graded;

    LearningEventType(boolean graded) {
        this.graded = graded;
    }

    /**
     * Seuls les evenements notes entrent dans le calcul de la maitrise.
     *
     * <p>Lire une lecon n'est pas la maitriser — compter une consultation comme
     * une reussite ferait grimper le score de quelqu'un qui se contente de faire
     * defiler les pages.
     */
    public boolean isGraded() {
        return graded;
    }
}
