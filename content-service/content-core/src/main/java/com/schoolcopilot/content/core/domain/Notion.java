package com.schoolcopilot.content.core.domain;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Une notion : le grain fin du programme, et l'unite de maitrise.
 *
 * <p>"Les limites d'une fonction", "l'accord du participe passe", "reconnaitre
 * les lettres de l'alphabet". C'est a ce niveau que tout le reste s'accrochera :
 * le suivi de progression, la revision espacee, la remediation, et le contenu sur
 * lequel l'assistant s'appuiera pour repondre.
 *
 * <p>Le grain compte. Trop grossier — le chapitre — et la remediation ne sait
 * plus quoi proposer. Trop fin, et le contenu devient impossible a produire.
 *
 * @param prerequisiteCodes les notions a maitriser avant celle-ci, dans le meme
 *        systeme. C'est ce graphe qui permet de dire "tu bloques sur les derivees
 *        parce que les limites ne sont pas acquises". Sans lui, rattraper un
 *        retard se resume a tout recommencer.
 */
@Document(collection = "notions")
@CompoundIndex(name = "idx_notion_system_code", def = "{'systemCode': 1, 'code': 1}", unique = true)
@CompoundIndex(name = "idx_notion_chapter", def = "{'systemCode': 1, 'chapterCode': 1, 'rank': 1}")
public record Notion(
        @Id String id,
        @Indexed String systemCode,
        @Indexed String chapterCode,
        String code,
        String label,
        String summary,
        int rank,
        List<String> prerequisiteCodes,
        PublicationStatus status,
        boolean archived) {

    public boolean isVisible() {
        return status == PublicationStatus.PUBLISHED && !archived;
    }

    public List<String> prerequisiteCodes() {
        return prerequisiteCodes == null ? List.of() : prerequisiteCodes;
    }
}
