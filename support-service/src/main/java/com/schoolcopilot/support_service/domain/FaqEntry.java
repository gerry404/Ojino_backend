package com.schoolcopilot.support_service.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Une question / reponse du centre d'aide.
 *
 * <p>Les deux index composes ne sont pas decoratifs. Le premier fait porter
 * l'unicite du code par Mongo lui-meme : une verification dans le service ne
 * suffit pas, deux requetes simultanees passeraient toutes les deux. Le second
 * est calque sur la requete la plus frequente du service — les entrees
 * publiees, non archivees, dans l'ordre — car un index compose se lit de gauche
 * a droite, comme un annuaire trie par nom puis prenom.
 *
 * @param code identifiant metier stable, ex. {@code CHANGER_CLASSE}. Immuable :
 *        des ecrans et des liens pointent dessus, le changer les casserait.
 * @param position ordre d'affichage a l'interieur d'une categorie
 * @param archived retiree du parcours eleve sans etre detruite. Le service
 *        n'expose aucune suppression : une entree effacee par erreur est
 *        irrecuperable, une entree archivee se restaure.
 */
@Document(collection = "faq_entries")
@CompoundIndex(name = "idx_faq_code", def = "{'code': 1}", unique = true)
@CompoundIndex(name = "idx_faq_listing",
        def = "{'status': 1, 'archived': 1, 'position': 1}")
public record FaqEntry(
        @Id String id,
        String code,
        String category,
        LocalizedText question,
        LocalizedText answer,
        int position,
        PublicationStatus status,
        boolean archived,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * Publiee et non archivee.
     *
     * <p>Deux conditions, un seul nom : c'est ce qui empeche d'en oublier une au
     * detour d'une requete ecrite six mois plus tard.
     */
    public boolean isVisible() {
        return status == PublicationStatus.PUBLISHED && !archived;
    }

    /**
     * Une nouvelle instance avec un autre statut.
     *
     * <p>Un {@code record} est immuable : ses champs sont finaux. On ne modifie
     * pas une entree, on en construit une autre. C'est plus verbeux, et c'est
     * voulu — personne ne peut alterer un objet a l'insu de celui qui le tient.
     */
    public FaqEntry withStatus(PublicationStatus newStatus) {
        return new FaqEntry(id, code, category, question, answer, position,
                newStatus, archived, createdAt, Instant.now());
    }

    public FaqEntry withArchived(boolean newArchived) {
        return new FaqEntry(id, code, category, question, answer, position,
                status, newArchived, createdAt, Instant.now());
    }

    /**
     * Une nouvelle instance avec un autre contenu.
     *
     * <p>Ni {@code code}, ni {@code status}, ni {@code archived} ne figurent
     * dans les parametres : une modification de texte ne doit pas pouvoir
     * republier une entree retiree, ni renommer son identifiant metier.
     */
    public FaqEntry withContent(String newCategory, LocalizedText newQuestion,
            LocalizedText newAnswer, int newPosition) {
        return new FaqEntry(id, code, newCategory, newQuestion, newAnswer,
                newPosition, status, archived, createdAt, Instant.now());
    }
}
