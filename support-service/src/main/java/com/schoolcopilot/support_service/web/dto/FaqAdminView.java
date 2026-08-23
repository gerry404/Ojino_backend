package com.schoolcopilot.support_service.web.dto;

import java.time.Instant;

import com.schoolcopilot.support_service.domain.FaqEntry;
import com.schoolcopilot.support_service.domain.LocalizedText;
import com.schoolcopilot.support_service.domain.PublicationStatus;

/**
 * Ce que voit le back-office : tout, statut et archivage compris.
 *
 * <p>Deuxieme vue sur la meme entite, et c'est exactement l'interet : le jour ou
 * l'admin veut un champ de plus, le parcours eleve ne bouge pas.
 */
public record FaqAdminView(
        String id,
        String code,
        String category,
        LocalizedText question,
        LocalizedText answer,
        int position,
        PublicationStatus status,
        boolean archived,
        Instant createdAt,
        Instant updatedAt) {

    public static FaqAdminView from(FaqEntry entry) {
        return new FaqAdminView(
                entry.id(),
                entry.code(),
                entry.category(),
                entry.question(),
                entry.answer(),
                entry.position(),
                entry.status(),
                entry.archived(),
                entry.createdAt(),
                entry.updatedAt());
    }
}
