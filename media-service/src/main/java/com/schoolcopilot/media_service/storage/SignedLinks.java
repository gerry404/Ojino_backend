package com.schoolcopilot.media_service.storage;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Signature des adresses temporaires du stockage local.
 *
 * <p>Reproduit le principe des adresses pre-signees de S3 : l'adresse porte une
 * date d'expiration et une signature. Quiconque modifie la cle ou la date invalide
 * la signature, et sans le secret il est impossible d'en forger une.
 *
 * <p>Sans cela, une adresse de stockage devinable suffirait a lire le devoir de
 * n'importe qui.
 */
public final class SignedLinks {

    private static final String ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final byte[] secret;

    public SignedLinks(String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    /** @return la signature d'une cle valable jusqu'a {@code expiresAt} */
    public String sign(String storageKey, String operation, long expiresAt) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            String payload = storageKey + "|" + operation + "|" + expiresAt;
            return ENCODER.encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("Signature impossible", e);
        }
    }

    public long expiryFor(Duration ttl) {
        return Instant.now().plus(ttl).getEpochSecond();
    }

    /**
     * Verifie une signature et sa date.
     *
     * <p>La comparaison est a temps constant : comparer deux signatures avec
     * {@code equals} laisse fuiter, par la duree, le nombre de caracteres corrects,
     * ce qui permet de la reconstituer octet par octet.
     */
    public boolean isValid(String storageKey, String operation, long expiresAt,
            String signature) {
        if (Instant.now().getEpochSecond() > expiresAt) {
            return false;
        }
        String expected = sign(storageKey, operation, expiresAt);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature == null ? new byte[0] : signature.getBytes(StandardCharsets.UTF_8));
    }
}
