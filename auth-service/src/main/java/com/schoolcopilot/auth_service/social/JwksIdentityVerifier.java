package com.schoolcopilot.auth_service.social;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.schoolcopilot.auth_service.config.AuthProperties;
import com.schoolcopilot.auth_service.exception.AuthException;

/**
 * Base commune a Google et Apple : tous deux publient un JWKS et signent des ID
 * tokens OpenID Connect, donc la verification est identique a l'URL et aux
 * emetteurs acceptes pres.
 *
 * <p>Le decodeur est construit paresseusement : au demarrage, aucun appel reseau
 * n'est fait, et un provider non configure ne fait rien echouer.
 */
abstract class JwksIdentityVerifier implements SocialIdentityVerifier {

    private static final Logger log = LoggerFactory.getLogger(JwksIdentityVerifier.class);

    private final AuthProperties.Social config;
    private final String jwkSetUri;
    private final Set<String> acceptedIssuers;

    private volatile JwtDecoder decoder;

    protected JwksIdentityVerifier(AuthProperties.Social config, String jwkSetUri,
            Set<String> acceptedIssuers) {
        this.config = config;
        this.jwkSetUri = jwkSetUri;
        this.acceptedIssuers = acceptedIssuers;
    }

    @Override
    public SocialUser verify(String idToken) {
        if (!config.isConfigured()) {
            throw AuthException.providerNotConfigured(provider().name());
        }
        try {
            return toSocialUser(decoder().decode(idToken));
        } catch (JwtException e) {
            log.debug("Token {} rejete : {}", provider(), e.getMessage());
            throw AuthException.invalidSocialToken(provider().name());
        }
    }

    /** Traduit les claims du provider en modele interne. */
    protected abstract SocialUser toSocialUser(Jwt jwt);

    private JwtDecoder decoder() {
        JwtDecoder local = decoder;
        if (local == null) {
            synchronized (this) {
                local = decoder;
                if (local == null) {
                    local = buildDecoder();
                    decoder = local;
                }
            }
        }
        return local;
    }

    private JwtDecoder buildDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                issuerValidator(),
                audienceValidator()));
        return jwtDecoder;
    }

    private OAuth2TokenValidator<Jwt> issuerValidator() {
        return jwt -> {
            String issuer = jwt.getIssuer() == null ? null : jwt.getIssuer().toString();
            return acceptedIssuers.contains(issuer)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                            new OAuth2Error("invalid_issuer", "Emetteur inattendu : " + issuer, null));
        };
    }

    /**
     * L'audience doit correspondre a l'un de nos client IDs : sans ce controle,
     * un token valide emis pour une autre application serait accepte.
     */
    private OAuth2TokenValidator<Jwt> audienceValidator() {
        return jwt -> {
            List<String> audience = jwt.getAudience();
            boolean matches = audience != null
                    && audience.stream().anyMatch(config.clientIds()::contains);
            return matches
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                            new OAuth2Error("invalid_audience",
                                    "Token emis pour une autre application : " + audience, null));
        };
    }

    /**
     * Apple envoie parfois {@code email_verified} sous forme de chaine plutot que
     * de booleen. On accepte les deux.
     */
    protected static boolean readBoolean(Jwt jwt, String claim) {
        Object value = jwt.getClaim(claim);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value instanceof String text && Boolean.parseBoolean(text);
    }
}
