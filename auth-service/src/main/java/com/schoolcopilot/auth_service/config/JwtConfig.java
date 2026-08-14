package com.schoolcopilot.auth_service.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Signature et verification de nos propres access tokens, en HS256.
 *
 * <p>Le secret est symetrique : les autres microservices (user-service, etc.)
 * valident les tokens en partageant simplement la meme valeur, sans appeler
 * l'auth-service a chaque requete. Le jour ou il y aura beaucoup de services,
 * on passera a RS256 avec un endpoint JWKS pour n'avoir plus qu'a distribuer une
 * cle publique.
 */
@Configuration
public class JwtConfig {

    @Bean
    public SecretKey accessTokenKey(AuthProperties properties) {
        byte[] secret = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException(
                    "ojino.auth.jwt.secret doit faire au moins 32 caracteres pour HS256 (actuellement "
                            + secret.length + ").");
        }
        return new SecretKeySpec(secret, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey accessTokenKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(accessTokenKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey accessTokenKey, AuthProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(accessTokenKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(properties.jwt().issuer())));
        return decoder;
    }
}
