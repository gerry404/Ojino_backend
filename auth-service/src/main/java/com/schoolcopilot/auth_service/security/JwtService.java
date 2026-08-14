package com.schoolcopilot.auth_service.security;

import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.schoolcopilot.auth_service.config.AuthProperties;
import com.schoolcopilot.auth_service.domain.User;

/**
 * Emet les access tokens.
 *
 * <p>Ils sont volontairement courts et sans etat : aucun appel a la base pour les
 * valider. C'est le refresh token, lui, qui est long et revocable.
 */
@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final AuthProperties properties;

    public JwtService(JwtEncoder encoder, AuthProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    /** Un access token signe, accompagne de sa date d'expiration. */
    public record AccessToken(String value, Instant expiresAt, long expiresInSeconds) {
    }

    public AccessToken issue(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.jwt().accessTokenTtl());

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getId())
                .claim("roles", List.copyOf(user.getRoles()));

        // Les claims nuls font echouer Nimbus : on n'ajoute que ce qu'on a.
        if (user.getEmail() != null) {
            claims.claim("email", user.getEmail());
        }
        if (user.getPhone() != null) {
            claims.claim("phone", user.getPhone());
        }
        if (user.getDisplayName() != null) {
            claims.claim("name", user.getDisplayName());
        }

        String value = encoder
                .encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        claims.build()))
                .getTokenValue();

        return new AccessToken(value, expiresAt,
                properties.jwt().accessTokenTtl().toSeconds());
    }
}
