package com.schoolcopilot.auth_service;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.schoolcopilot.auth_service.config.AuthProperties;
import com.schoolcopilot.auth_service.domain.User;

/** Objets de configuration prets a l'emploi pour les tests unitaires. */
public final class TestFixtures {

    private TestFixtures() {
    }

    public static AuthProperties properties() {
        return new AuthProperties(
                new AuthProperties.Jwt(
                        "secret-de-test-suffisamment-long-pour-hs256-0123456789",
                        "ojino-auth-test",
                        Duration.ofMinutes(30)),
                new AuthProperties.Refresh(Duration.ofDays(365), Duration.ofDays(90)),
                new AuthProperties.Cookie("ojino_rt", "/api/v1/auth", "Lax", false, ""),
                new AuthProperties.Otp(6, Duration.ofMinutes(5), 5, Duration.ofSeconds(60), true),
                new AuthProperties.Social(List.of("google-client-id")),
                new AuthProperties.Social(List.of("apple-client-id")),
                new AuthProperties.Cors(List.of("http://localhost:3000")),
                List.of());
    }

    public static User user(String id) {
        User user = new User();
        user.setId(id);
        user.setEmail("eleve@example.com");
        user.setDisplayName("Eleve Test");
        user.setRoles(new LinkedHashSet<>(Set.of("USER")));
        return user;
    }
}
