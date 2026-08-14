package com.schoolcopilot.media_service.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La signature est ce qui empeche de lire le devoir de n'importe qui en devinant
 * une adresse. Ces tests couvrent les manieres de la contourner.
 */
class SignedLinksTest {

    private static final String KEY = "homework/user-1/abc123.jpg";

    private final SignedLinks links = new SignedLinks("un-secret-de-test-suffisamment-long-32+");

    @Test
    @DisplayName("une signature fraiche est acceptee")
    void freshSignatureIsAccepted() {
        long expiresAt = links.expiryFor(Duration.ofMinutes(15));

        assertThat(links.isValid(KEY, "GET", expiresAt, links.sign(KEY, "GET", expiresAt)))
                .isTrue();
    }

    @Test
    @DisplayName("une signature expiree est refusee")
    void expiredSignatureIsRejected() {
        long expired = Instant.now().minusSeconds(1).getEpochSecond();

        assertThat(links.isValid(KEY, "GET", expired, links.sign(KEY, "GET", expired))).isFalse();
    }

    @Test
    @DisplayName("changer la cle invalide la signature")
    void tamperingWithTheKeyIsDetected() {
        long expiresAt = links.expiryFor(Duration.ofMinutes(15));
        String signature = links.sign(KEY, "GET", expiresAt);

        // Sans cela, il suffirait de remplacer l'identifiant dans l'adresse pour
        // lire le fichier de quelqu'un d'autre.
        assertThat(links.isValid("homework/user-2/abc123.jpg", "GET", expiresAt, signature))
                .isFalse();
    }

    @Test
    @DisplayName("repousser la date d'expiration invalide la signature")
    void tamperingWithTheExpiryIsDetected() {
        long expiresAt = links.expiryFor(Duration.ofMinutes(15));
        String signature = links.sign(KEY, "GET", expiresAt);

        assertThat(links.isValid(KEY, "GET", expiresAt + 86400, signature)).isFalse();
    }

    @Test
    @DisplayName("une adresse de lecture ne vaut pas pour un envoi")
    void operationsAreNotInterchangeable() {
        long expiresAt = links.expiryFor(Duration.ofMinutes(15));
        String readSignature = links.sign(KEY, "GET", expiresAt);

        // Sinon une adresse de telechargement partagee permettrait d'ecraser le
        // fichier qu'elle designe.
        assertThat(links.isValid(KEY, "PUT", expiresAt, readSignature)).isFalse();
    }

    @Test
    @DisplayName("une signature d'un autre secret est refusee")
    void signatureFromAnotherSecretIsRejected() {
        long expiresAt = links.expiryFor(Duration.ofMinutes(15));
        String forged = new SignedLinks("un-autre-secret-tout-aussi-long-mais-faux")
                .sign(KEY, "GET", expiresAt);

        assertThat(links.isValid(KEY, "GET", expiresAt, forged)).isFalse();
    }

    @Test
    @DisplayName("une signature absente est refusee")
    void missingSignatureIsRejected() {
        long expiresAt = links.expiryFor(Duration.ofMinutes(15));

        assertThat(links.isValid(KEY, "GET", expiresAt, null)).isFalse();
        assertThat(links.isValid(KEY, "GET", expiresAt, "")).isFalse();
    }
}
