package com.schoolcopilot.support_service.web.dto;

import com.schoolcopilot.support_service.domain.FaqEntry;
import com.schoolcopilot.support_service.domain.LocalizedText;

/**
 * Ce que voit un utilisateur.
 *
 * <p>Ni statut, ni archivage, ni dates : le parcours eleve n'a aucune raison
 * d'apprendre qu'il existe des brouillons. Le DTO est un contrat, le domaine une
 * implementation ; les lier reviendrait a s'interdire de renommer un champ sans
 * casser les applications deja installees.
 */
public record FaqEntryView(
        String code,
        String category,
        LocalizedText question,
        LocalizedText answer,
        int position) {

    /** La conversion vit dans le DTO, jamais dans le controleur. */
    public static FaqEntryView from(FaqEntry entry) {
        return new FaqEntryView(
                entry.code(),
                entry.category(),
                entry.question(),
                entry.answer(),
                entry.position());
    }
}
