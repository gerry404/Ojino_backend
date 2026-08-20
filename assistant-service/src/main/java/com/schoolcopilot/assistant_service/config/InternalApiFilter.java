package com.schoolcopilot.assistant_service.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Protege les routes internes par un secret partage.
 *
 * <p>Les autres services declenchent des notifications depuis des taches
 * planifiees, sans jeton d'utilisateur sous la main : le mecanisme habituel — le
 * jeton de l'appelant — ne s'applique pas ici.
 *
 * <p><strong>C'est une solution d'attente, et il faut le dire.</strong> Un secret
 * partage ne distingue pas les services entre eux : celui qui le detient peut tout
 * faire. Le jour ou les services se multiplieront, il faudra une vraie identite de
 * service — mTLS, ou des jetons obtenus par client credentials. En attendant, ce
 * filtre vaut mieux qu'une route interne ouverte.
 */
@Component
public class InternalApiFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Internal-Token";
    private static final String PROTECTED_PREFIX = "/api/v1/internal/";

    private final byte[] expected;

    public InternalApiFilter(SecurityProperties properties) {
        this.expected = properties.internalToken().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PROTECTED_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String provided = request.getHeader(HEADER);

        // Comparaison a temps constant : avec equals, la duree laisse fuiter le
        // nombre de caracteres corrects, ce qui permet de reconstituer le secret.
        boolean valid = provided != null && MessageDigest.isEqual(expected,
                provided.getBytes(StandardCharsets.UTF_8));

        if (!valid) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":\"invalid_internal_token\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
