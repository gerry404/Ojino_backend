package com.schoolcopilot.auth_service.web.client;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.schoolcopilot.auth_service.config.AuthProperties;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Le refresh token cote web.
 *
 * <p>Il est depose dans un cookie {@code httpOnly} : le JavaScript de la page ne
 * peut pas le lire, ce qui le met hors d'atteinte d'une injection XSS. C'est la
 * raison pour laquelle le web ne recoit pas son refresh token dans le corps de la
 * reponse, contrairement au mobile qui, lui, dispose d'un keystore securise.
 */
@Component
public class RefreshCookies {

    private final AuthProperties properties;

    public RefreshCookies(AuthProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie issue(String value, Duration maxAge) {
        return base(value).maxAge(maxAge).build();
    }

    /** Cookie vide et immediatement expire : utilise a la deconnexion. */
    public ResponseCookie clear() {
        return base("").maxAge(Duration.ZERO).build();
    }

    public Optional<String> read(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> properties.cookie().name().equals(cookie.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        AuthProperties.Cookie config = properties.cookie();
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(config.name(), value)
                .httpOnly(true)
                .secure(config.secure())
                .path(config.path())
                .sameSite(config.sameSite());
        if (config.domain() != null && !config.domain().isBlank()) {
            builder.domain(config.domain());
        }
        return builder;
    }
}
