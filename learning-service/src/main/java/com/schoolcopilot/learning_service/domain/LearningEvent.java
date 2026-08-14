package com.schoolcopilot.learning_service.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Un fait d'apprentissage : ce que l'eleve a fait, quand, et avec quel resultat.
 *
 * <p><strong>Rien ne se modifie ni ne se supprime ici.</strong> Les evenements
 * s'ajoutent, et la maitrise se recalcule a partir d'eux. C'est ce qui permet de
 * corriger l'algorithme de notation plus tard et de rejouer tout l'historique —
 * impossible si on ne gardait qu'un score mis a jour en place.
 *
 * <p>C'est aussi le service au plus fort volume d'ecriture du projet : un eleve
 * actif produit des dizaines d'evenements par seance.
 *
 * @param score de 0 a 1. Un exercice juste ou faux vaut 1 ou 0 ; un QCM partiel
 *        prend une valeur intermediaire.
 * @param weight importance relative de l'evenement. Un devoir surveille doit
 *        peser plus qu'un exercice d'entrainement.
 */
@Document(collection = "learning_events")
@CompoundIndex(name = "idx_event_user_notion",
        def = "{'userId': 1, 'systemCode': 1, 'notionCode': 1, 'occurredAt': -1}")
public record LearningEvent(
        @Id String id,
        @Indexed String userId,
        String systemCode,
        String notionCode,
        LearningEventType type,
        double score,
        double weight,
        String sourceCode,
        Integer durationSeconds,
        Instant occurredAt) {

    /** Vrai si l'evenement porte un resultat chiffre, par opposition a une simple consultation. */
    public boolean isGraded() {
        return type.isGraded();
    }
}
