package com.schoolcopilot.learning_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Les seuils du calcul de maitrise.
 *
 * <p>Externalises volontairement : ce sont des reglages pedagogiques, pas des
 * constantes techniques. Ils s'ajusteront a l'usage, en observant de vrais
 * eleves, et cela ne doit pas demander de redeployer.
 *
 * @param minAttempts en dessous, on ne conclut pas — une notion ne devient pas
 *        acquise sur un seul coup de chance
 * @param recencyWeight poids du dernier resultat face a la moyenne. Plus il est
 *        haut, plus la maitrise suit les derniers resultats plutot que toute
 *        l'annee.
 */
@ConfigurationProperties(prefix = "ojino.mastery")
public record MasteryProperties(
        double masteredThreshold,
        double strugglingThreshold,
        int minAttempts,
        double recencyWeight) {
}
