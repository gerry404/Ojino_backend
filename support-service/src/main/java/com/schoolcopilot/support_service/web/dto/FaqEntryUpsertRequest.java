package com.schoolcopilot.support_service.web.dto;

import java.time.Instant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.schoolcopilot.support_service.domain.FaqEntry;
import com.schoolcopilot.support_service.domain.LocalizedText;
import com.schoolcopilot.support_service.domain.PublicationStatus;

/**
 * Ce que le back-office envoie pour creer ou modifier une entree.
 *
 * <p>Aucun champ {@code status} ni {@code archived} : ce ne sont pas des donnees
 * saisies, ce sont des etats que seul le service fait evoluer, par des routes
 * dediees. Les accepter ici laisserait un client publier sans relecture.
 *
 * <p>Le {@code @Valid} sur les champs imbriques est indispensable : sans lui,
 * Spring verifie que {@code question} n'est pas nulle et s'arrete la — un texte
 * entierement vide passerait.
 */
public record FaqEntryUpsertRequest(

        @NotBlank
        String code,

        @NotBlank
        String category,

        @NotNull @Valid
        LocalizedText question,

        @NotNull @Valid
        LocalizedText answer,

        @Min(0)
        int position) {

    /**
     * L'entree correspondante, en brouillon.
     *
     * <p>{@code id} reste nul : Mongo genere l'identifiant technique.
     */
    public FaqEntry toDomain() {
        Instant now = Instant.now();
        return new FaqEntry(
                null,
                code,
                category,
                question,
                answer,
                position,
                PublicationStatus.DRAFT,
                false,
                now,
                now);
    }
}
