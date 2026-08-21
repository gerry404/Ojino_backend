package com.schoolcopilot.support_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.schoolcopilot.support_service.domain.FaqEntry;
import com.schoolcopilot.support_service.domain.PublicationStatus;

/**
 * Les acces base du service, regroupes.
 *
 * <p>Un fichier plutot que dix d'une ligne : la convention est la meme dans
 * {@code notification-service} et {@code learning-service}.
 */
public final class SupportRepositories {

    private SupportRepositories() {
    }

    /**
     * Aucune implementation a ecrire : Spring Data lit le nom des methodes et
     * fabrique la classe au demarrage.
     *
     * <p>Le revers, c'est qu'une faute de frappe dans un nom de champ n'est pas
     * vue par le compilateur. Elle explose au demarrage du contexte, avec un
     * {@code No property 'xxx' found for type FaqEntry}.
     */
    @Repository
    public interface Faqs extends MongoRepository<FaqEntry, String> {

        /** Le parcours eleve : ce qui est publie, non archive, dans l'ordre. */
        List<FaqEntry> findByStatusAndArchivedFalseOrderByPositionAsc(
                PublicationStatus status);

        /** Le meme, restreint a une categorie. */
        List<FaqEntry> findByStatusAndCategoryAndArchivedFalseOrderByPositionAsc(
                PublicationStatus status, String category);

        /**
         * Le back-office voit tout, brouillons et archives compris, groupe par
         * categorie pour rester lisible sans pagination.
         */
        List<FaqEntry> findAllByOrderByCategoryAscPositionAsc();

        Optional<FaqEntry> findByCode(String code);

        /**
         * Refuse un doublon avant l'ecriture, pour rendre un 409 propre plutot
         * qu'une erreur Mongo brute. Ne remplace pas l'index unique : deux
         * requetes simultanees passeraient ce test toutes les deux.
         */
        boolean existsByCode(String code);
    }
}
