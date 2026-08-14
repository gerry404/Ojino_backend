package com.schoolcopilot.content.core.spi;

import java.util.List;

import com.schoolcopilot.content.core.domain.EducationCycle;

/**
 * Le contrat qu'implemente chaque module de cycle.
 *
 * <p>C'est la seule chose que {@code content-core} connait d'un cycle. Il ignore
 * qu'il existe un lycee ou une universite : il voit des implementations de cette
 * interface, ramassees par Spring au demarrage.
 *
 * <p>Ajouter un pays avec un cycle inedit, ou retirer la maternelle pour en faire
 * une application separee, ne demande donc de toucher ni au coeur ni aux autres
 * cycles.
 */
public interface CurriculumModule {

    EducationCycle cycle();

    /** Libelle lisible, affiche par les applications. */
    String label();

    /**
     * Les choix que ce cycle impose apres celui de la classe, dans l'ordre.
     *
     * <p>Le lycee renvoie {@code [TRACK, SUBJECTS]} : filiere puis matieres. La
     * maternelle renvoie {@code [LEARNING_DOMAINS]}. L'universite ne parle ni de
     * filiere ni de matiere mais de parcours et d'unites d'enseignement.
     */
    List<CurriculumStep> steps();

    /** Ce a quoi les chapitres de ce cycle se rattachent. */
    AnchorKind anchorKind();

    /**
     * Vrai si ce code d'ancrage existe et peut encore servir dans ce systeme.
     *
     * <p>Le coeur s'en sert pour refuser un chapitre rattache a une matiere ou a
     * une unite d'enseignement qui n'existe pas. Il ne sait pas comment verifier :
     * seul le module du cycle connait le referentiel concerne.
     */
    boolean anchorExists(String systemCode, String anchorCode);
}
