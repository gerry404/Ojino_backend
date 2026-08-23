package com.schoolcopilot.support_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reglages du centre d'aide.
 */
@ConfigurationProperties(prefix = "ojino.support")
public record SupportProperties(Faq faq) {

    /**
     * @param publicAccess la FAQ est-elle lisible sans etre connecte. Externalise
     *        parce que c'est une decision produit qui peut changer sans que le
     *        code change : une FAQ ouverte se referencerait sur Google, une FAQ
     *        fermee ne dit rien de ton produit a un concurrent.
     */
    public record Faq(boolean publicAccess) {
    }
}
