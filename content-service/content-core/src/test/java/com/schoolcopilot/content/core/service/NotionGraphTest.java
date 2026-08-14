package com.schoolcopilot.content.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schoolcopilot.content.core.domain.Notion;
import com.schoolcopilot.content.core.domain.PublicationStatus;

/**
 * Le graphe est de l'algorithmique pure : il se verifie sans base ni Spring.
 *
 * <p>Le programme de test reproduit une chaine reelle de mathematiques :
 * <pre>
 *   SUITES ─┐
 *           ├─> LIMITES ─> DERIVEES ─> ETUDE_FONCTION
 *   FONCTIONS ┘                ┌───────────┘
 *   CALCUL_LITTERAL ─> FONCTIONS
 * </pre>
 */
class NotionGraphTest {

    private final NotionGraph graph = NotionGraph.of(List.of(
            notion("CALCUL_LITTERAL"),
            notion("SUITES"),
            notion("FONCTIONS", "CALCUL_LITTERAL"),
            notion("LIMITES", "SUITES", "FONCTIONS"),
            notion("DERIVEES", "LIMITES"),
            notion("ETUDE_FONCTION", "DERIVEES")));

    @Test
    @DisplayName("le parcours de rattrapage remonte toute la chaine")
    void learningPathWalksTheWholeChain() {
        assertThat(graph.learningPath("DERIVEES"))
                .containsExactlyInAnyOrder("CALCUL_LITTERAL", "FONCTIONS", "SUITES", "LIMITES");
    }

    @Test
    @DisplayName("chaque notion du parcours arrive apres ses propres prerequis")
    void learningPathRespectsDependencies() {
        List<String> path = graph.learningPath("ETUDE_FONCTION");

        // On verifie l'invariant, pas une sequence exacte : plusieurs ordres sont
        // valables, et en figer un rendrait le test faux au premier changement de
        // parcours sans qu'un seul comportement ait bouge.
        assertThat(path.indexOf("CALCUL_LITTERAL")).isLessThan(path.indexOf("FONCTIONS"));
        assertThat(path.indexOf("FONCTIONS")).isLessThan(path.indexOf("LIMITES"));
        assertThat(path.indexOf("SUITES")).isLessThan(path.indexOf("LIMITES"));
        assertThat(path.indexOf("LIMITES")).isLessThan(path.indexOf("DERIVEES"));
    }

    @Test
    @DisplayName("la notion demandee ne figure pas dans son propre parcours")
    void theNotionItselfIsExcluded() {
        assertThat(graph.learningPath("ETUDE_FONCTION")).doesNotContain("ETUDE_FONCTION");
    }

    @Test
    @DisplayName("une notion sans prerequis a un parcours vide")
    void rootNotionHasNoPath() {
        assertThat(graph.learningPath("CALCUL_LITTERAL")).isEmpty();
    }

    @Test
    @DisplayName("un prerequis partage n'apparait qu'une fois")
    void sharedPrerequisiteAppearsOnce() {
        // CALCUL_LITTERAL est atteint par plusieurs chemins depuis ETUDE_FONCTION.
        assertThat(graph.learningPath("ETUDE_FONCTION"))
                .containsOnlyOnce("CALCUL_LITTERAL");
    }

    @Test
    @DisplayName("une notion ne peut pas etre son propre prerequis")
    void selfReferenceIsACycle() {
        assertThat(graph.wouldCreateCycle("LIMITES", List.of("LIMITES"))).isTrue();
    }

    @Test
    @DisplayName("un prerequis qui depend deja de la notion ferme une boucle")
    void indirectCycleIsDetected() {
        // DERIVEES depend de LIMITES. Declarer DERIVEES comme prerequis de LIMITES
        // rendrait les deux inatteignables : chacune attendrait l'autre.
        assertThat(graph.wouldCreateCycle("LIMITES", List.of("DERIVEES"))).isTrue();
        assertThat(graph.wouldCreateCycle("LIMITES", List.of("ETUDE_FONCTION"))).isTrue();
    }

    @Test
    @DisplayName("un prerequis legitime est accepte")
    void legitimatePrerequisiteIsAccepted() {
        assertThat(graph.wouldCreateCycle("ETUDE_FONCTION", List.of("SUITES"))).isFalse();
    }

    @Test
    @DisplayName("un seul candidat fautif suffit a refuser tout le lot")
    void oneBadCandidateRejectsTheWholeSet() {
        assertThat(graph.wouldCreateCycle("LIMITES", List.of("SUITES", "DERIVEES"))).isTrue();
    }

    private Notion notion(String code, String... prerequisites) {
        return new Notion("CM-FR:" + code, "CM-FR", "CHAP", code, code, null, 1,
                List.of(prerequisites), PublicationStatus.PUBLISHED, false);
    }
}
