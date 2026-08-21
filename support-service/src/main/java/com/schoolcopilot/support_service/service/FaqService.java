package com.schoolcopilot.support_service.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.schoolcopilot.support_service.domain.FaqEntry;
import com.schoolcopilot.support_service.domain.PublicationStatus;
import com.schoolcopilot.support_service.exception.ApiException;
import com.schoolcopilot.support_service.repository.SupportRepositories.Faqs;
import com.schoolcopilot.support_service.web.dto.FaqEntryUpsertRequest;

/**
 * Les regles du centre d'aide.
 *
 * <p>Rien de HTTP ici, rien de Mongo non plus : ce qui est ecrit dans cette
 * classe resterait vrai si on remplacait REST par GraphQL ou Mongo par
 * PostgreSQL.
 */
@Service
public class FaqService {

    private final Faqs faqs;

    public FaqService(Faqs faqs) {
        this.faqs = faqs;
    }

    // -----------------------------------------------------------------------
    // Parcours eleve
    // -----------------------------------------------------------------------

    /**
     * Ce qui est publie et non archive. La categorie est facultative.
     *
     * <p>Le filtre de visibilite est pose ici, dans la requete, et non chez
     * l'appelant : c'est le seul endroit ou l'oublier est impossible.
     */
    public List<FaqEntry> listVisible(String category) {
        if (category == null || category.isBlank()) {
            return faqs.findByStatusAndArchivedFalseOrderByPositionAsc(
                    PublicationStatus.PUBLISHED);
        }
        return faqs.findByStatusAndCategoryAndArchivedFalseOrderByPositionAsc(
                PublicationStatus.PUBLISHED, category);
    }

    /**
     * Les categories qui ont au moins une entree visible.
     *
     * <p>Deduites du contenu plutot que declarees dans une enumeration : une
     * categorie vide n'a aucune raison d'apparaitre a l'ecran.
     */
    public List<String> listCategories() {
        return listVisible(null).stream()
                .map(FaqEntry::category)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
    }

    // -----------------------------------------------------------------------
    // Back-office
    // -----------------------------------------------------------------------

    /** Tout, brouillons et archives compris. */
    public List<FaqEntry> listAll() {
        return faqs.findAllByOrderByCategoryAscPositionAsc();
    }

    public FaqEntry getByCode(String code) {
        return find(code);
    }

    /**
     * Creation.
     *
     * <p>Le statut n'est pas negociable : une entree fraichement saisie n'a ete
     * relue par personne, la publier d'emblee mettrait sous les yeux d'un
     * utilisateur un texte que personne n'a verifie.
     */
    public FaqEntry create(FaqEntryUpsertRequest request) {
        if (faqs.existsByCode(request.code())) {
            throw ApiException.faqCodeAlreadyExists(request.code());
        }
        return faqs.save(request.toDomain());
    }

    /**
     * Modification du contenu.
     *
     * <p>Le {@code code} present dans la requete est volontairement ignore :
     * c'est un identifiant metier, le changer casserait les liens qui pointent
     * dessus. Qui veut un autre code archive et recree.
     */
    public FaqEntry update(String code, FaqEntryUpsertRequest request) {
        FaqEntry existing = find(code);
        return faqs.save(existing.withContent(
                request.category(),
                request.question(),
                request.answer(),
                request.position()));
    }

    public FaqEntry publish(String code) {
        return faqs.save(find(code).withStatus(PublicationStatus.PUBLISHED));
    }

    public FaqEntry unpublish(String code) {
        return faqs.save(find(code).withStatus(PublicationStatus.DRAFT));
    }

    /**
     * Archivage.
     *
     * <p>C'est la seule facon de retirer une entree du parcours eleve. Le
     * service n'expose aucune suppression : une entree effacee par erreur est
     * irrecuperable, une entree archivee se restaure.
     */
    public FaqEntry archive(String code) {
        return faqs.save(find(code).withArchived(true));
    }

    public FaqEntry restore(String code) {
        return faqs.save(find(code).withArchived(false));
    }

    private FaqEntry find(String code) {
        return faqs.findByCode(code)
                .orElseThrow(() -> ApiException.faqEntryNotFound(code));
    }
}
