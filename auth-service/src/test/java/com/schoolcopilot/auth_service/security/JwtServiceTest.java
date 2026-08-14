package com.schoolcopilot.auth_service.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import com.schoolcopilot.auth_service.TestFixtures;
import com.schoolcopilot.auth_service.config.AuthProperties;
import com.schoolcopilot.auth_service.config.JwtConfig;
import com.schoolcopilot.auth_service.domain.User;

class JwtServiceTest {

    private final JwtConfig jwtConfig = new JwtConfig();
    private final AuthProperties properties = TestFixtures.properties();
    private final SecretKey key = jwtConfig.accessTokenKey(properties);
    private final JwtService jwtService =
            new JwtService(jwtConfig.jwtEncoder(key), properties);

    @Test
    @DisplayName("l'access token emis est relisible et porte l'identite de l'utilisateur")
    void issuedTokenIsVerifiable() {
        User user = TestFixtures.user("user-42");

        JwtService.AccessToken issued = jwtService.issue(user);
        Jwt decoded = jwtConfig.jwtDecoder(key, properties).decode(issued.value());

        assertThat(decoded.getSubject()).isEqualTo("user-42");
        assertThat(decoded.getClaimAsString("email")).isEqualTo("eleve@example.com");
        assertThat(decoded.getClaimAsStringList("roles")).containsExactly("USER");
        assertThat(issued.expiresInSeconds()).isEqualTo(1800);
    }

    @Test
    @DisplayName("un compte sans email ni telephone produit quand meme un token valide")
    void tokenWithoutOptionalClaims() {
        User user = new User();
        user.setId("user-sans-email");

        JwtService.AccessToken issued = jwtService.issue(user);
        Jwt decoded = jwtConfig.jwtDecoder(key, properties).decode(issued.value());

        assertThat(decoded.getSubject()).isEqualTo("user-sans-email");
        assertThat(decoded.getClaimAsString("email")).isNull();
    }

    @Test
    @DisplayName("un token signe avec un autre secret est rejete")
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService.AccessToken issued = jwtService.issue(TestFixtures.user("user-1"));

        AuthProperties other = new AuthProperties(
                new AuthProperties.Jwt("un-tout-autre-secret-de-trente-deux-caracteres-mini",
                        properties.jwt().issuer(), properties.jwt().accessTokenTtl()),
                properties.refresh(), properties.cookie(), properties.otp(),
                properties.google(), properties.apple(), properties.cors());

        assertThatThrownBy(() -> jwtConfig
                .jwtDecoder(jwtConfig.accessTokenKey(other), other)
                .decode(issued.value()))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("un secret trop court est refuse des le demarrage plutot que de fragiliser la signature")
    void shortSecretIsRefused() {
        AuthProperties weak = new AuthProperties(
                new AuthProperties.Jwt("trop-court", "ojino", properties.jwt().accessTokenTtl()),
                properties.refresh(), properties.cookie(), properties.otp(),
                properties.google(), properties.apple(), properties.cors());

        assertThatThrownBy(() -> jwtConfig.accessTokenKey(weak))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }
}
