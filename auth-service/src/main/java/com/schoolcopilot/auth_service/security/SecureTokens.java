package com.schoolcopilot.auth_service.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generation de secrets aleatoires et empreintes, partagees par les refresh
 * tokens et les codes SMS.
 */
public final class SecureTokens {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private SecureTokens() {
    }

    /** Un secret opaque de 256 bits, sur mesure pour un refresh token. */
    public static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }

    /** Un code numerique de {@code length} chiffres, zeros de tete compris. */
    public static String randomNumericCode(int length) {
        StringBuilder code = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            code.append(RANDOM.nextInt(10));
        }
        return code.toString();
    }

    /**
     * Empreinte SHA-256 en hexadecimal. Pas de BCrypt ici : ces valeurs sont deja
     * aleatoires et a forte entropie, un hachage lent n'apporterait rien et
     * couterait cher a chaque rafraichissement.
     */
    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 introuvable", e);
        }
    }

    /** Comparaison a temps constant, pour ne pas fuiter le code via la duree. */
    public static boolean matches(String rawValue, String expectedHash) {
        if (rawValue == null || expectedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(
                sha256(rawValue).getBytes(StandardCharsets.UTF_8),
                expectedHash.getBytes(StandardCharsets.UTF_8));
    }
}
