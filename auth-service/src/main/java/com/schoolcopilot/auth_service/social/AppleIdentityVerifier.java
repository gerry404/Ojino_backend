package com.schoolcopilot.auth_service.social;

import java.util.Set;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.schoolcopilot.auth_service.config.AuthProperties;
import com.schoolcopilot.auth_service.domain.AuthProvider;

/**
 * Verification des identity tokens Sign in with Apple.
 *
 * <p>Apple ne met jamais le nom de l'utilisateur dans le token : il n'est transmis
 * qu'une seule fois, par le SDK client, au tout premier accord. C'est pourquoi
 * l'application doit nous le passer a part dans le champ {@code displayName} de la
 * requete de connexion.
 */
@Component
public class AppleIdentityVerifier extends JwksIdentityVerifier {

    private static final String JWK_SET_URI = "https://appleid.apple.com/auth/keys";
    private static final Set<String> ISSUERS = Set.of("https://appleid.apple.com");

    public AppleIdentityVerifier(AuthProperties properties) {
        super(properties.apple(), JWK_SET_URI, ISSUERS);
    }

    @Override
    public AuthProvider provider() {
        return AuthProvider.APPLE;
    }

    @Override
    protected SocialUser toSocialUser(Jwt jwt) {
        return new SocialUser(
                AuthProvider.APPLE,
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                readBoolean(jwt, "email_verified"),
                null,
                null);
    }
}
