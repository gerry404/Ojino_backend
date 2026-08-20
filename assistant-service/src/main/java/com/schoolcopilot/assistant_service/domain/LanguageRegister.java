package com.schoolcopilot.assistant_service.domain;

/**
 * Le registre de langage attendu de l'assistant.
 *
 * <p>C'est la contrainte la plus dure du produit. La meme notion expliquee a un
 * enfant de grande section et a un etudiant de prepa n'a rien a voir : ni le
 * vocabulaire, ni la longueur, ni le ton.
 *
 * <p>Ce n'est pas un parametre qu'on ajoute a la fin. Il fait partie de la
 * requete envoyee au moteur, et il derive du cycle scolaire — jamais de l'age
 * seul, car un eleve de 22 ans qui reprend une Terminale doit recevoir le
 * registre de la Terminale.
 */
public enum LanguageRegister {

    /** Maternelle et CP. */
    EARLY_YEARS(
            "Phrases tres courtes. Vocabulaire concret et familier. Aucun terme technique. "
                    + "Ton chaleureux et encourageant. Toujours partir d'un exemple que l'enfant "
                    + "peut voir ou toucher. Une seule idee a la fois.",
            2, 400),

    /** College. */
    COLLEGE(
            "Phrases simples et directes. Un concept a la fois, illustre par un exemple du "
                    + "quotidien. Le vocabulaire disciplinaire est introduit et explique, jamais "
                    + "suppose connu. Ton encourageant.",
            3, 800),

    /** Lycee. */
    HIGH_SCHOOL(
            "Vocabulaire disciplinaire assume. Raisonnement structure et explicite. Les etapes "
                    + "sont montrees, pas seulement le resultat. Ton respectueux, sans "
                    + "condescendance.",
            5, 1500),

    /** Classes preparatoires. */
    PREPA(
            "Rigueur et densite. Notations formelles attendues. Les etapes evidentes peuvent "
                    + "etre passees. Aller au fond du raisonnement plutot que de simplifier.",
            6, 2000),

    /** Superieur. */
    UNIVERSITY(
            "Registre academique. Nuance et references assumees. Autonomie supposee : orienter "
                    + "vers les sources plutot que tout deployer.",
            6, 2000);

    private final String guidance;
    private final int maxSentencesPerIdea;
    private final int maxReplyChars;

    LanguageRegister(String guidance, int maxSentencesPerIdea, int maxReplyChars) {
        this.guidance = guidance;
        this.maxSentencesPerIdea = maxSentencesPerIdea;
        this.maxReplyChars = maxReplyChars;
    }

    /** La consigne transmise au moteur. */
    public String guidance() {
        return guidance;
    }

    public int maxSentencesPerIdea() {
        return maxSentencesPerIdea;
    }

    /**
     * Longueur maximale attendue d'une reponse.
     *
     * <p>Une reponse de deux mille caracteres a un enfant de six ans est un echec,
     * meme si chaque mot est juste : il ne la lira pas.
     */
    public int maxReplyChars() {
        return maxReplyChars;
    }

    /**
     * Derive le registre du cycle scolaire.
     *
     * <p>Un cycle inconnu retombe sur le college : c'est le registre le moins
     * risque. Trop simple pour un lyceen reste comprehensible ; trop complexe pour
     * un enfant ne l'est pas.
     */
    public static LanguageRegister forCycle(String cycle) {
        if (cycle == null) {
            return COLLEGE;
        }
        return switch (cycle) {
            case "EARLY_YEARS" -> EARLY_YEARS;
            case "COLLEGE" -> COLLEGE;
            case "HIGH_SCHOOL" -> HIGH_SCHOOL;
            case "PREPA" -> PREPA;
            case "UNIVERSITY" -> UNIVERSITY;
            default -> COLLEGE;
        };
    }
}
