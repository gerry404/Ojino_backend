package com.schoolcopilot.auth_service.social;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.schoolcopilot.auth_service.domain.AuthProvider;
import com.schoolcopilot.auth_service.exception.AuthException;

/**
 * Aiguille vers le bon verificateur. Ajouter Facebook ou Microsoft demain ne
 * demandera qu'une nouvelle implementation de {@link SocialIdentityVerifier} :
 * elle sera ramassee automatiquement.
 */
@Component
public class SocialVerifiers {

    private final Map<AuthProvider, SocialIdentityVerifier> byProvider =
            new EnumMap<>(AuthProvider.class);

    public SocialVerifiers(List<SocialIdentityVerifier> verifiers) {
        verifiers.forEach(verifier -> byProvider.put(verifier.provider(), verifier));
    }

    public SocialIdentityVerifier forProvider(AuthProvider provider) {
        SocialIdentityVerifier verifier = byProvider.get(provider);
        if (verifier == null) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "unsupported_provider",
                    "Connexion " + provider + " non prise en charge.");
        }
        return verifier;
    }
}
