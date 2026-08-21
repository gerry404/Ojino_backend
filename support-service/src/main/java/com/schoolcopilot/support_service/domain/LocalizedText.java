package com.schoolcopilot.support_service.domain;

import jakarta.validation.constraints.NotBlank;

/**
 * Un texte porte en francais et en anglais.
 *
 * <p>Aucune annotation Mongo ici : cet objet n'est jamais stocke seul, il est
 * imbrique dans le document parent. Mongo l'ecrit comme un sous-objet JSON, la
 * ou une base relationnelle aurait exige une seconde table.
 *
 * @param fr obligatoire : c'est la langue de repli, elle ne peut pas manquer
 * @param en facultatif : une traduction en retard ne doit pas empecher de
 *        publier une entree
 */
public record LocalizedText(@NotBlank String fr, String en) {

    /**
     * Rend le texte de la langue demandee, avec repli sur le francais.
     *
     * <p>Le repli n'est pas un detail de confort : une traduction manquante
     * afficherait sinon une case blanche a l'utilisateur, ce qui est pire qu'une
     * reponse dans la mauvaise langue.
     */
    public String forLanguage(String language) {
        if ("en".equalsIgnoreCase(language) && en != null && !en.isBlank()) {
            return en;
        }
        return fr;
    }
}
