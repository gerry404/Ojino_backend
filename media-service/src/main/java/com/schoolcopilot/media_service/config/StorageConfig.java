package com.schoolcopilot.media_service.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.schoolcopilot.media_service.storage.LocalMediaStorage;
import com.schoolcopilot.media_service.storage.SignedLinks;

@Configuration
public class StorageConfig {

    @Bean
    SignedLinks signedLinks(MediaProperties properties) {
        if (properties.signingSecret().length() < 32) {
            throw new IllegalStateException(
                    "ojino.media.signing-secret doit faire au moins 32 caracteres : "
                            + "c'est lui qui empeche de forger une adresse de telechargement.");
        }
        return new SignedLinks(properties.signingSecret());
    }

    /**
     * Le stockage sur disque, actif tant qu'aucun autre n'est choisi.
     *
     * <p>La condition porte sur une propriete et non sur la presence d'un autre
     * bean : {@code @ConditionalOnBean} depend de l'ordre de traitement des
     * configurations et ne vaut de facon fiable que dans une autoconfiguration.
     * Le choix du stockage est explicite, il n'a pas a etre devine.
     *
     * <p>Le type de retour est volontairement concret : c'est ce qui permet au
     * controleur de transfert de dependre de cette implementation precise.
     */
    @Bean
    @ConditionalOnProperty(name = "ojino.media.storage", havingValue = "local",
            matchIfMissing = true)
    LocalMediaStorage localMediaStorage(MediaProperties properties, SignedLinks signedLinks) {
        return new LocalMediaStorage(properties, signedLinks);
    }
}
