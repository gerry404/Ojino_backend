package com.schoolcopilot.auth_service.service;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.schoolcopilot.auth_service.domain.AuthProvider;
import com.schoolcopilot.auth_service.domain.User;
import com.schoolcopilot.auth_service.exception.AuthException;
import com.schoolcopilot.auth_service.repository.UserRepository;
import com.schoolcopilot.auth_service.social.SocialUser;

/**
 * Trouve ou cree le compte derriere une identite, quelle que soit la porte
 * d'entree utilisee.
 *
 * <p>C'est ici que se joue la convergence demandee : quelqu'un qui s'inscrit par
 * Google avec {@code paul@gmail.com} puis revient plus tard par email doit
 * retomber sur le meme compte, pas en creer un second.
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private static final String DEFAULT_ROLE = "USER";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    /** Un compte, et l'information de savoir s'il vient d'etre cree (pour lancer l'onboarding). */
    public record ResolvedAccount(User user, boolean created) {
    }

    // ------------------------------------------------------------------
    // Email + mot de passe
    // ------------------------------------------------------------------

    public ResolvedAccount registerWithEmail(String rawEmail, String rawPassword, String displayName) {
        String email = normalizeEmail(rawEmail);
        if (users.existsByEmail(email)) {
            throw AuthException.emailAlreadyUsed();
        }

        User user = newUser();
        user.setEmail(email);
        // L'email n'est pas encore prouve. On laisse quand meme entrer : bloquer
        // l'acces derriere un lien de confirmation ferait fuir la moitie des
        // inscriptions. La verification viendra plus tard, sans bloquer l'usage.
        user.setEmailVerified(false);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setDisplayName(displayName);
        user.linkIdentity(AuthProvider.EMAIL, email);

        return new ResolvedAccount(save(user), true);
    }

    public User authenticateWithEmail(String rawEmail, String rawPassword) {
        String email = normalizeEmail(rawEmail);
        User user = users.findByEmail(email).orElseThrow(AuthException::invalidCredentials);

        if (user.getPasswordHash() == null) {
            throw AuthException.noPasswordSet();
        }
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw AuthException.invalidCredentials();
        }
        requireEnabled(user);
        return user;
    }

    // ------------------------------------------------------------------
    // Telephone (apres validation du code SMS)
    // ------------------------------------------------------------------

    public ResolvedAccount resolveByPhone(String phone) {
        Optional<User> existing = users.findByPhone(phone);
        if (existing.isPresent()) {
            User user = existing.get();
            requireEnabled(user);
            // Le code SMS vient d'etre valide : le numero est prouve.
            user.setPhoneVerified(true);
            user.linkIdentity(AuthProvider.PHONE, phone);
            return new ResolvedAccount(save(user), false);
        }

        User user = newUser();
        user.setPhone(phone);
        user.setPhoneVerified(true);
        user.linkIdentity(AuthProvider.PHONE, phone);
        return new ResolvedAccount(save(user), true);
    }

    // ------------------------------------------------------------------
    // Google / Apple
    // ------------------------------------------------------------------

    public ResolvedAccount resolveBySocial(SocialUser social, String displayNameHint) {
        // 1. Deja connu de ce provider : c'est le cas courant.
        Optional<User> byIdentity = users.findByIdentity(social.provider(), social.subject());
        if (byIdentity.isPresent()) {
            User user = byIdentity.get();
            requireEnabled(user);
            enrich(user, social, displayNameHint);
            return new ResolvedAccount(save(user), false);
        }

        // 2. Premiere connexion via ce provider, mais l'email est deja celui d'un
        //    compte existant : on rattache au lieu de dupliquer.
        //    Condition imperative : le provider affirme avoir verifie l'email.
        //    Sans cette garantie, n'importe qui pourrait se declarer proprietaire
        //    d'une adresse et s'emparer du compte correspondant.
        if (social.email() != null && social.emailVerified()) {
            Optional<User> byEmail = users.findByEmail(normalizeEmail(social.email()));
            if (byEmail.isPresent()) {
                User user = byEmail.get();
                requireEnabled(user);
                user.linkIdentity(social.provider(), social.subject());
                user.setEmailVerified(true);
                enrich(user, social, displayNameHint);
                log.info("Provider {} rattache au compte existant {}", social.provider(), user.getId());
                return new ResolvedAccount(save(user), false);
            }
        }

        // 3. Nouvel utilisateur.
        User user = newUser();
        user.linkIdentity(social.provider(), social.subject());
        if (social.email() != null) {
            user.setEmail(normalizeEmail(social.email()));
            user.setEmailVerified(social.emailVerified());
        }
        enrich(user, social, displayNameHint);
        return new ResolvedAccount(save(user), true);
    }

    public User requireById(String userId) {
        User user = users.findById(userId).orElseThrow(AuthException::userNotFound);
        requireEnabled(user);
        return user;
    }

    public User touchLastLogin(User user) {
        user.setLastLoginAt(Instant.now());
        return save(user);
    }

    // ------------------------------------------------------------------

    private void enrich(User user, SocialUser social, String displayNameHint) {
        // Apple ne transmet le nom qu'a la toute premiere connexion, via le client :
        // on le prend quand il arrive et on ne l'ecrase jamais ensuite.
        if (user.getDisplayName() == null) {
            user.setDisplayName(social.displayName() != null ? social.displayName() : displayNameHint);
        }
        if (user.getAvatarUrl() == null && social.pictureUrl() != null) {
            user.setAvatarUrl(social.pictureUrl());
        }
    }

    private User newUser() {
        User user = new User();
        user.setRoles(new java.util.LinkedHashSet<>(Set.of(DEFAULT_ROLE)));
        user.setCreatedAt(Instant.now());
        return user;
    }

    private User save(User user) {
        user.setUpdatedAt(Instant.now());
        try {
            return users.save(user);
        } catch (DuplicateKeyException e) {
            // Deux inscriptions simultanees pour la meme identite : l'index unique
            // a tranche. On renvoie une erreur claire plutot qu'une 500.
            throw AuthException.emailAlreadyUsed();
        }
    }

    private void requireEnabled(User user) {
        if (user.isDisabled()) {
            throw AuthException.accountDisabled();
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
