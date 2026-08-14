package com.schoolcopilot.auth_service.social;

import java.util.Set;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.schoolcopilot.auth_service.config.AuthProperties;
import com.schoolcopilot.auth_service.domain.AuthProvider;

/** Verification des ID tokens Google Sign-In. */
@Component
public class GoogleIdentityVerifier extends JwksIdentityVerifier {

    private static final String JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";

    /** Google emet l'un ou l'autre selon l'anciennete du SDK client. */
    private static final Set<String> ISSUERS =
            Set.of("https://accounts.google.com", "accounts.google.com");

    public GoogleIdentityVerifier(AuthProperties properties) {
        super(properties.google(), JWK_SET_URI, ISSUERS);
    }

    @Override
    public AuthProvider provider() {
        return AuthProvider.GOOGLE;
    }

    @Override
    protected SocialUser toSocialUser(Jwt jwt) {
        return new SocialUser(
                AuthProvider.GOOGLE,
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                readBoolean(jwt, "email_verified"),
                jwt.getClaimAsString("name"),
                jwt.getClaimAsString("picture"));
    }
}
