package com.schoolcopilot.assistant_service.domain;

import java.util.List;

/**
 * Ce que l'on sait de l'eleve au moment ou il pose sa question.
 *
 * <p>C'est ce qui distingue un assistant scolaire d'un agent conversationnel
 * generique : la reponse s'appuie sur son niveau, sur ce qu'il maitrise deja, et
 * sur le programme de sa classe.
 *
 * @param cycle cycle scolaire, d'ou derive le registre de langage
 * @param strugglingNotions notions ou il bloque. Permet d'expliquer a partir de
 *        ce qu'il maitrise plutot que de supposer acquis ce qui ne l'est pas.
 * @param notionLabel la notion sur laquelle porte la conversation, si connue
 * @param prerequisites ce qui precede cette notion dans le programme
 * @param contentAvailable faux quand content-service n'a pas repondu. La reponse
 *        sera moins ancree, et la tracabilite doit le dire.
 */
public record StudyContext(
        String systemCode,
        String levelCode,
        String levelLabel,
        String cycle,
        String trackCode,
        List<String> subjects,
        List<String> strugglingNotions,
        String notionCode,
        String notionLabel,
        String notionSummary,
        List<String> prerequisites,
        boolean contentAvailable) {

    public List<String> subjects() {
        return subjects == null ? List.of() : subjects;
    }

    public List<String> strugglingNotions() {
        return strugglingNotions == null ? List.of() : strugglingNotions;
    }

    public List<String> prerequisites() {
        return prerequisites == null ? List.of() : prerequisites;
    }

    public LanguageRegister register() {
        return LanguageRegister.forCycle(cycle);
    }

    /** Taille approximative, pour l'estimation du cout avant appel. */
    public int approximateSize() {
        int size = length(levelLabel) + length(notionLabel) + length(notionSummary);
        for (String subject : subjects()) {
            size += subject.length();
        }
        for (String notion : strugglingNotions()) {
            size += notion.length();
        }
        for (String prerequisite : prerequisites()) {
            size += prerequisite.length();
        }
        return size;
    }

    private int length(String value) {
        return value == null ? 0 : value.length();
    }
}
