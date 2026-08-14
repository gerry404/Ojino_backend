package com.schoolcopilot.auth_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.schoolcopilot.auth_service.domain.AuthProvider;
import com.schoolcopilot.auth_service.domain.User;
import com.schoolcopilot.auth_service.exception.AuthException;
import com.schoolcopilot.auth_service.repository.UserRepository;
import com.schoolcopilot.auth_service.service.AccountService.ResolvedAccount;
import com.schoolcopilot.auth_service.social.SocialUser;

/**
 * La regle metier la plus delicate du service : decider quand deux identites
 * designent la meme personne.
 */
class AccountServiceTest {

    private final Map<String, User> stored = new LinkedHashMap<>();
    private UserRepository users;
    private AccountService accounts;

    @BeforeEach
    void setUp() {
        stored.clear();
        users = mock(UserRepository.class);

        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(UUID.randomUUID().toString());
            }
            stored.put(user.getId(), user);
            return user;
        });
        when(users.findByEmail(anyString())).thenAnswer(invocation -> stored.values().stream()
                .filter(user -> invocation.getArgument(0).equals(user.getEmail()))
                .findFirst());
        when(users.existsByEmail(anyString())).thenAnswer(invocation -> stored.values().stream()
                .anyMatch(user -> invocation.getArgument(0).equals(user.getEmail())));
        when(users.findByPhone(anyString())).thenAnswer(invocation -> stored.values().stream()
                .filter(user -> invocation.getArgument(0).equals(user.getPhone()))
                .findFirst());
        when(users.findByIdentity(any(AuthProvider.class), anyString()))
                .thenAnswer(invocation -> stored.values().stream()
                        .filter(user -> user.getIdentities().stream()
                                .anyMatch(identity -> identity.provider() == invocation.getArgument(0)
                                        && identity.subject().equals(invocation.getArgument(1))))
                        .findFirst());
        when(users.findById(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(stored.get(invocation.getArgument(0))));

        accounts = new AccountService(users, new BCryptPasswordEncoder());
    }

    // ------------------------------------------------------------------
    // Email + mot de passe
    // ------------------------------------------------------------------

    @Test
    @DisplayName("l'inscription hache le mot de passe et normalise l'email")
    void registerHashesPasswordAndNormalizesEmail() {
        ResolvedAccount account = accounts.registerWithEmail("  Paul@Example.COM ", "motdepasse1", "Paul");

        assertThat(account.created()).isTrue();
        assertThat(account.user().getEmail()).isEqualTo("paul@example.com");
        assertThat(account.user().getPasswordHash())
                .isNotNull()
                .isNotEqualTo("motdepasse1");
        assertThat(account.user().hasIdentityFor(AuthProvider.EMAIL)).isTrue();
    }

    @Test
    @DisplayName("deux inscriptions avec le meme email sont refusees")
    void registerRejectsDuplicateEmail() {
        accounts.registerWithEmail("paul@example.com", "motdepasse1", "Paul");

        assertThatThrownBy(() ->
                accounts.registerWithEmail("PAUL@example.com", "autrepass1", "Paul bis"))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("code", "email_already_used");
    }

    @Test
    @DisplayName("un mot de passe faux et un email inconnu donnent la meme erreur")
    void wrongPasswordAndUnknownEmailAreIndistinguishable() {
        accounts.registerWithEmail("paul@example.com", "motdepasse1", "Paul");

        assertThatThrownBy(() -> accounts.authenticateWithEmail("paul@example.com", "mauvais"))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("code", "invalid_credentials");

        assertThatThrownBy(() -> accounts.authenticateWithEmail("inconnu@example.com", "motdepasse1"))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("code", "invalid_credentials");
    }

    @Test
    @DisplayName("un compte cree par Google ne peut pas etre force par mot de passe")
    void socialAccountHasNoPassword() {
        accounts.resolveBySocial(google("google-sub-1", "paul@example.com", true), null);

        assertThatThrownBy(() -> accounts.authenticateWithEmail("paul@example.com", "nimporte"))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("code", "no_password_set");
    }

    @Test
    @DisplayName("un compte desactive ne peut plus se connecter")
    void disabledAccountIsRefused() {
        ResolvedAccount account = accounts.registerWithEmail("paul@example.com", "motdepasse1", "Paul");
        account.user().setDisabled(true);

        assertThatThrownBy(() -> accounts.authenticateWithEmail("paul@example.com", "motdepasse1"))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("code", "account_disabled");
    }

    // ------------------------------------------------------------------
    // Convergence des identites
    // ------------------------------------------------------------------

    @Test
    @DisplayName("revenir par Google retombe sur le meme compte")
    void returningGoogleUserFindsTheSameAccount() {
        ResolvedAccount first = accounts.resolveBySocial(
                google("google-sub-1", "paul@example.com", true), null);
        ResolvedAccount second = accounts.resolveBySocial(
                google("google-sub-1", "paul@example.com", true), null);

        assertThat(first.created()).isTrue();
        assertThat(second.created()).isFalse();
        assertThat(second.user().getId()).isEqualTo(first.user().getId());
        assertThat(stored).hasSize(1);
    }

    @Test
    @DisplayName("Google sur l'email d'un compte existant rattache au lieu de dupliquer")
    void googleLinksToExistingAccountWithSameVerifiedEmail() {
        ResolvedAccount byEmail =
                accounts.registerWithEmail("paul@example.com", "motdepasse1", "Paul");

        ResolvedAccount bySocial = accounts.resolveBySocial(
                google("google-sub-1", "paul@example.com", true), null);

        assertThat(bySocial.created()).isFalse();
        assertThat(bySocial.user().getId()).isEqualTo(byEmail.user().getId());
        assertThat(bySocial.user().hasIdentityFor(AuthProvider.EMAIL)).isTrue();
        assertThat(bySocial.user().hasIdentityFor(AuthProvider.GOOGLE)).isTrue();
        assertThat(bySocial.user().isEmailVerified()).isTrue();
        assertThat(stored).hasSize(1);
    }

    @Test
    @DisplayName("un email non verifie par le provider ne donne jamais acces a un compte existant")
    void unverifiedProviderEmailNeverTakesOverAnAccount() {
        ResolvedAccount victim =
                accounts.registerWithEmail("paul@example.com", "motdepasse1", "Paul");

        // Un provider qui ne garantit pas l'email pourrait sinon servir a s'emparer
        // du compte de quelqu'un d'autre en declarant simplement son adresse.
        ResolvedAccount attacker = accounts.resolveBySocial(
                google("google-sub-attaquant", "paul@example.com", false), null);

        assertThat(attacker.created()).isTrue();
        assertThat(attacker.user().getId()).isNotEqualTo(victim.user().getId());
        assertThat(stored).hasSize(2);
    }

    @Test
    @DisplayName("Apple sans email cree un compte autonome, sans rien casser")
    void appleWithoutEmailStillWorks() {
        ResolvedAccount account = accounts.resolveBySocial(
                new SocialUser(AuthProvider.APPLE, "apple-sub-1", null, false, null, null),
                "Paul depuis Apple");

        assertThat(account.created()).isTrue();
        assertThat(account.user().getEmail()).isNull();
        assertThat(account.user().getDisplayName()).isEqualTo("Paul depuis Apple");
        assertThat(account.user().hasIdentityFor(AuthProvider.APPLE)).isTrue();
    }

    @Test
    @DisplayName("le nom deja connu n'est jamais ecrase par le provider")
    void existingDisplayNameIsPreserved() {
        accounts.registerWithEmail("paul@example.com", "motdepasse1", "Paul Martin");

        ResolvedAccount linked = accounts.resolveBySocial(
                google("google-sub-1", "paul@example.com", true), "Autre Nom");

        assertThat(linked.user().getDisplayName()).isEqualTo("Paul Martin");
    }

    // ------------------------------------------------------------------
    // Telephone
    // ------------------------------------------------------------------

    @Test
    @DisplayName("un numero inconnu cree le compte a la volee, comme sur WhatsApp")
    void unknownPhoneCreatesAccount() {
        ResolvedAccount account = accounts.resolveByPhone("+237690000000");

        assertThat(account.created()).isTrue();
        assertThat(account.user().isPhoneVerified()).isTrue();
        assertThat(account.user().hasIdentityFor(AuthProvider.PHONE)).isTrue();
    }

    @Test
    @DisplayName("un numero deja connu retrouve son compte")
    void knownPhoneReturnsTheSameAccount() {
        ResolvedAccount first = accounts.resolveByPhone("+237690000000");
        ResolvedAccount second = accounts.resolveByPhone("+237690000000");

        assertThat(second.created()).isFalse();
        assertThat(second.user().getId()).isEqualTo(first.user().getId());
        assertThat(stored).hasSize(1);
    }

    private SocialUser google(String subject, String email, boolean emailVerified) {
        return new SocialUser(AuthProvider.GOOGLE, subject, email, emailVerified, null, null);
    }
}
