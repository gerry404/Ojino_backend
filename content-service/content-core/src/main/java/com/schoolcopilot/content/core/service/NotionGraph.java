package com.schoolcopilot.content.core.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.schoolcopilot.content.core.domain.Notion;

/**
 * Le graphe des prerequis d'un systeme, en memoire.
 *
 * <p>Volontairement separe du service : ce sont deux algorithmes, ils se lisent
 * et se verifient seuls, sans base ni Spring autour.
 *
 * <p>Le graphe doit rester acyclique. Un cycle — A avant B, B avant A — rendrait
 * l'ordre d'apprentissage impossible a calculer et bloquerait l'eleve pour de
 * bon : chaque notion attendrait l'autre.
 */
final class NotionGraph {

    /** Code de notion vers ses prerequis directs. */
    private final Map<String, List<String>> prerequisites;

    private NotionGraph(Map<String, List<String>> prerequisites) {
        this.prerequisites = prerequisites;
    }

    static NotionGraph of(List<Notion> notions) {
        Map<String, List<String>> edges = new java.util.HashMap<>();
        notions.forEach(notion -> edges.put(notion.code(), notion.prerequisiteCodes()));
        return new NotionGraph(edges);
    }

    /**
     * Vrai si declarer ces prerequis pour cette notion fermerait une boucle.
     *
     * <p>Deux cas : la notion se cite elle-meme, ou l'un des prerequis proposes
     * depend deja d'elle, directement ou non.
     */
    boolean wouldCreateCycle(String notionCode, List<String> candidates) {
        for (String candidate : candidates) {
            if (candidate.equals(notionCode) || reaches(candidate, notionCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tout ce qu'il faut maitriser avant cette notion, dans un ordre ou chaque
     * element arrive apres ses propres prerequis.
     *
     * <p>C'est le parcours de rattrapage : un eleve qui bloque sur les derivees
     * recoit la liste des notions a reprendre, dans l'ordre ou les reprendre.
     *
     * <p>Parcours en profondeur, resultat en post-ordre — les dependances sortent
     * naturellement avant ce qui en depend. La notion demandee n'y figure pas.
     */
    List<String> learningPath(String notionCode) {
        List<String> ordered = new ArrayList<>();
        Set<String> visited = new LinkedHashSet<>();
        visit(notionCode, visited, ordered);
        ordered.remove(notionCode);
        return ordered;
    }

    private void visit(String code, Set<String> visited, List<String> ordered) {
        if (!visited.add(code)) {
            return;
        }
        for (String prerequisite : prerequisites.getOrDefault(code, List.of())) {
            visit(prerequisite, visited, ordered);
        }
        ordered.add(code);
    }

    /** Existe-t-il un chemin de {@code from} vers {@code target} ? */
    private boolean reaches(String from, String target) {
        Set<String> seen = new LinkedHashSet<>();
        Deque<String> stack = new ArrayDeque<>();
        stack.push(from);

        while (!stack.isEmpty()) {
            String current = stack.pop();
            if (current.equals(target)) {
                return true;
            }
            if (!seen.add(current)) {
                continue;
            }
            prerequisites.getOrDefault(current, List.of()).forEach(stack::push);
        }
        return false;
    }
}
