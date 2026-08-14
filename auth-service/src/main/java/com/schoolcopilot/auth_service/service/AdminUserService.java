package com.schoolcopilot.auth_service.service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.schoolcopilot.auth_service.domain.RefreshToken;
import com.schoolcopilot.auth_service.domain.Role;
import com.schoolcopilot.auth_service.domain.User;
import com.schoolcopilot.auth_service.exception.AuthException;
import com.schoolcopilot.auth_service.repository.RefreshTokenRepository;
import com.schoolcopilot.auth_service.repository.UserRepository;
import com.schoolcopilot.auth_service.security.TokenService;

/**
 * Les operations du back-office sur les comptes.
 *
 * <p>Separe volontairement d'{@link AccountService} : celui-ci sert les parcours
 * des applications, celui-la le pilotage par l'equipe. Melanger les deux
 * reviendrait a exposer, dans le meme objet, des methodes qu'un utilisateur peut
 * declencher et d'autres qu'il ne doit jamais atteindre.
 */
@Service
public class AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserService.class);

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final TokenService tokenService;

    public AdminUserService(UserRepository users, RefreshTokenRepository refreshTokens,
            TokenService tokenService) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.tokenService = tokenService;
    }

    /** Liste paginee, filtree par un terme libre quand il est fourni. */
    public Page<User> search(String term, Pageable pageable) {
        if (term == null || term.isBlank()) {
            return users.findAll(pageable);
        }
        // Le terme part dans une expression reguliere Mongo : sans echappement, un
        // point ou une etoile changerait le sens de la recherche, et une expression
        // bien choisie pourrait bloquer le serveur.
        return users.search(Pattern.quote(term.trim()), pageable);
    }

    public User get(String userId) {
        return users.findById(userId).orElseThrow(AuthException::userNotFound);
    }

    /**
     * Desactive un compte et coupe toutes ses sessions.
     *
     * <p>Sans la revocation, l'utilisateur resterait connecte jusqu'a l'expiration
     * de son access token : la desactivation serait sans effet immediat.
     */
    public User disable(String userId, String actingAdminId) {
        if (userId.equals(actingAdminId)) {
            throw AuthException.cannotActOnSelf("desactiver");
        }
        User user = get(userId);
        user.setDisabled(true);
        tokenService.revokeAllForUser(userId);
        log.info("Compte {} desactive par l'administrateur {}.", userId, actingAdminId);
        return save(user);
    }

    public User enable(String userId) {
        User user = get(userId);
        user.setDisabled(false);
        return save(user);
    }

    public User updateRoles(String userId, Set<String> requestedRoles, String actingAdminId) {
        Set<String> roles = requestedRoles.stream()
                .map(role -> role.trim().toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        Set<String> unknown = new LinkedHashSet<>(roles);
        unknown.removeAll(Role.KNOWN);
        if (!unknown.isEmpty()) {
            throw AuthException.unknownRole(unknown);
        }

        if (userId.equals(actingAdminId) && !roles.contains(Role.ADMIN)) {
            throw AuthException.cannotActOnSelf("retirer le role administrateur de");
        }

        User user = get(userId);
        user.setRoles(roles);
        log.info("Roles du compte {} passes a {} par l'administrateur {}.", userId, roles,
                actingAdminId);
        return save(user);
    }

    /** Les sessions encore ouvertes : ni consommees, ni revoquees, ni expirees. */
    public List<RefreshToken> activeSessions(String userId) {
        get(userId);
        Instant now = Instant.now();
        return refreshTokens.findByUserId(userId).stream()
                .filter(token -> token.isUsable(now))
                .sorted((a, b) -> b.getIssuedAt().compareTo(a.getIssuedAt()))
                .toList();
    }

    /** Deconnecte le compte de tous ses appareils. */
    public void revokeSessions(String userId) {
        get(userId);
        tokenService.revokeAllForUser(userId);
        log.info("Toutes les sessions du compte {} ont ete revoquees.", userId);
    }

    private User save(User user) {
        user.setUpdatedAt(Instant.now());
        return users.save(user);
    }
}
