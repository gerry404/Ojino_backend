package com.schoolcopilot.media_service.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reglages du stockage et des adresses signees.
 *
 * @param publicBaseUrl adresse par laquelle les clients joignent ce service. Sert
 *        a construire les adresses signees du stockage local.
 * @param uploadTtl duree de validite d'une adresse d'envoi. Courte : elle ne sert
 *        qu'a un transfert immediat.
 * @param downloadTtl duree de validite d'une adresse de lecture.
 * @param pendingTtl delai au-dela duquel un envoi jamais confirme est considere
 *        abandonne et nettoye.
 */
@ConfigurationProperties(prefix = "ojino.media")
public record MediaProperties(
        String publicBaseUrl,
        String signingSecret,
        Duration uploadTtl,
        Duration downloadTtl,
        Duration pendingTtl,
        Local local) {

    /** @param directory racine du stockage sur disque, en developpement */
    public record Local(String directory) {
    }
}
