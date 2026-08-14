package com.schoolcopilot.auth_service.otp;

import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;

import com.schoolcopilot.auth_service.exception.AuthException;

/**
 * Normalisation des numeros de telephone.
 *
 * <p>On impose le format international E.164 ({@code +237690000000}) : c'est le seul
 * moyen qu'un numero identifie un compte de facon stable, quel que soit le pays
 * depuis lequel l'utilisateur se connecte.
 */
public final class PhoneNumbers {

    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");

    private PhoneNumbers() {
    }

    /** Retire espaces, points, tirets et parentheses, puis valide le format. */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw invalid();
        }
        String cleaned = raw.replaceAll("[\\s.()\\-]", "");
        if (!E164.matcher(cleaned).matches()) {
            throw invalid();
        }
        return cleaned;
    }

    private static AuthException invalid() {
        return new AuthException(HttpStatus.BAD_REQUEST, "invalid_phone",
                "Numero invalide. Utilisez le format international, par exemple +237690000000.");
    }
}
