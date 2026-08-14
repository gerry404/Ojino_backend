package com.schoolcopilot.auth_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.schoolcopilot.auth_service.TestFixtures;
import com.schoolcopilot.auth_service.domain.ClientType;
import com.schoolcopilot.auth_service.domain.RefreshToken;
import com.schoolcopilot.auth_service.domain.Role;
import com.schoolcopilot.auth_service.domain.User;
import com.schoolcopilot.auth_service.exception.AuthException;
import com.schoolcopilot.auth_service.repository.RefreshTokenRepository;
import com.schoolcopilot.auth_service.repository.UserRepository;
import com.schoolcopilot.auth_service.security.TokenService;
import com.schoolcopilot.auth_service.security.TokenService.DeviceContext;

class AdminUserServiceTest {

    private static final String ADMIN_ID = "admin-1";
    private static final String TARGET_ID = "user-2";

    private final Map<String, User> storedUsers = new LinkedHashMap<>();
    private final Map<String, RefreshToken> storedTokens = new LinkedHashMap<>();

    private UserRepository users;
    private RefreshTokenRepository refreshTokens;
    private TokenService tokenService;
    private AdminUserService adminUsers;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        storedUsers.clear();
        storedTokens.clear();

        users = mock(UserRepository.class);
        refreshTokens = mock(RefreshTokenRepository.class);

        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            storedUsers.put(user.getId(), user);
            return user;
        });
        when(users.findById(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(storedUsers.get(invocation.getArgument(0))));

        when(refreshTokens.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            if (token.getId() == null) {
                token.setId(UUID.randomUUID().toString());
            }
            storedTokens.put(token.getId(), token);
            return token;
        });
        when(refreshTokens.saveAll(any())).thenAnswer(invocation -> {
            List<RefreshToken> tokens =
                    new ArrayList<>((Collection<RefreshToken>) invocation.getArgument(0));
            tokens.forEach(token -> storedTokens.put(token.getId(), token));
            return tokens;
        });
        when(refreshTokens.findByUserId(anyString())).thenAnswer(invocation ->
                storedTokens.values().stream()
                        .filter(token -> invocation.getArgument(0).equals(token.getUserId()))
                        .toList());

        tokenService = new TokenService(refreshTokens, TestFixtures.properties());
        adminUsers = new AdminUserService(users, refreshTokens, tokenService);

        storedUsers.put(ADMIN_ID, userWithRoles(ADMIN_ID, Role.USER, Role.ADMIN));
        storedUsers.put(TARGET_ID, userWithRoles(TARGET_ID, Role.USER));
    }

    // ------------------------------------------------------------------
    // Recherche
    // ------------------------------------------------------------------

    @Test
    @DisplayName("sans terme, la recherche liste simplement les comptes")
    void blankTermListsEverything() {
        Pageable pageable = PageRequest.of(0, 20);
        when(users.findAll(pageable)).thenReturn(new PageImpl<>(List.of()));

        adminUsers.search("  ", pageable);

        verify(users).findAll(pageable);
        verify(users, never()).search(anyString(), any());
    }

    @Test
    @DisplayName("le terme recherche est echappe avant de partir dans l'expression reguliere")
    void searchTermIsEscaped() {
        Pageable pageable = PageRequest.of(0, 20);
        when(users.search(anyString(), eq(pageable))).thenReturn(new PageImpl<>(List.of()));

        // Sans echappement, ce terme serait une expression reguliere valide et
        // couteuse au lieu d'une chaine recherchee litteralement.
        adminUsers.search("(a+)+$", pageable);

        ArgumentCaptor<String> term = ArgumentCaptor.forClass(String.class);
        verify(users).search(term.capture(), eq(pageable));
        assertThat(term.getValue()).isEqualTo(Pattern.quote("(a+)+$"));
    }

    // ------------------------------------------------------------------
    // Desactivation
    // ------------------------------------------------------------------

    @Test
    @DisplayName("desactiver un compte coupe aussi ses sessions en cours")
    void disablingAlsoRevokesSessions() {
        User target = storedUsers.get(TARGET_ID);
        tokenService.startSession(target, ClientType.MOBILE, DeviceContext.unknown());

        User disabled = adminUsers.disable(TARGET_ID, ADMIN_ID);

        // Sans cette revocation, le compte resterait utilisable jusqu'a l'expiration
        // de son access token.
        assertThat(disabled.isDisabled()).isTrue();
        assertThat(storedTokens.values()).allMatch(RefreshToken::isRevoked);
    }

    @Test
    @DisplayName("un administrateur ne peut pas se desactiver lui-meme")
    void adminCannotDisableThemselves() {
        assertThatThrownBy(() -> adminUsers.disable(ADMIN_ID, ADMIN_ID))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("code", "cannot_act_on_self");

        assertThat(storedUsers.get(ADMIN_ID).isDisabled()).isFalse();
    }

    @Test
    @DisplayName("reactiver un compte le rend de nouveau utilisable")
    void enablingRestoresTheAccount() {
        adminUsers.disable(TARGET_ID, ADMIN_ID);

        assertThat(adminUsers.enable(TARGET_ID).isDisabled()).isFalse();
    }

    @Test
    @DisplayName("agir sur un compte inexistant renvoie une erreur claire")
    void unknownUserIsReported() {
        assertThatThrownBy(() -> adminUsers.get("fantome"))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("code", "user_not_found");
    }

    // ------------------------------------------------------------------
    // Roles
    // ------------------------------------------------------------------

    @Test
    @DisplayName("promouvoir un compte lui ajoute le role administrateur")
    void rolesCanBeGranted() {
        User promoted = adminUsers.updateRoles(TARGET_ID, Set.of("USER", "ADMIN"), ADMIN_ID);

        assertThat(promoted.getRoles()).containsExactlyInAnyOrder(Role.USER, Role.ADMIN);
    }

    @Test
    @DisplayName("la casse des roles est normalisee")
    void roleCaseIsNormalized() {
        User promoted = adminUsers.updateRoles(TARGET_ID, Set.of("user", "Admin"), ADMIN_ID);

        assertThat(promoted.getRoles()).containsExactlyInAnyOrder(Role.USER, Role.ADMIN);
    }

    @Test
    @DisplayName("un role invente est refuse")
    void unknownRoleIsRejected() {
        assertThatThrownBy(() ->
                adminUsers.updateRoles(TARGET_ID, Set.of("USER", "SUPERADMIN"), ADMIN_ID))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("code", "unknown_role");
    }

    @Test
    @DisplayName("un administrateur ne peut pas se retirer son propre role")
    void adminCannotDemoteThemselves() {
        // Sinon le dernier administrateur peut verrouiller toute l'equipe hors du
        // back-office, sans autre recours qu'une intervention en base.
        assertThatThrownBy(() -> adminUsers.updateRoles(ADMIN_ID, Set.of("USER"), ADMIN_ID))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("code", "cannot_act_on_self");

        assertThat(storedUsers.get(ADMIN_ID).getRoles()).contains(Role.ADMIN);
    }

    @Test
    @DisplayName("un administrateur peut modifier ses autres roles en gardant ADMIN")
    void adminCanChangeTheirOtherRoles() {
        User updated = adminUsers.updateRoles(ADMIN_ID, Set.of("ADMIN"), ADMIN_ID);

        assertThat(updated.getRoles()).containsExactly(Role.ADMIN);
    }

    // ------------------------------------------------------------------
    // Sessions
    // ------------------------------------------------------------------

    @Test
    @DisplayName("seules les sessions encore ouvertes sont listees")
    void onlyUsableSessionsAreListed() {
        User target = storedUsers.get(TARGET_ID);
        TokenService.IssuedRefreshToken active =
                tokenService.startSession(target, ClientType.MOBILE, DeviceContext.unknown());
        TokenService.IssuedRefreshToken expired =
                tokenService.startSession(target, ClientType.WEB, DeviceContext.unknown());
        expired.stored().setExpiresAt(Instant.now().minusSeconds(1));

        List<RefreshToken> sessions = adminUsers.activeSessions(TARGET_ID);

        assertThat(sessions).extracting(RefreshToken::getId)
                .containsExactly(active.stored().getId());
    }

    @Test
    @DisplayName("revoquer les sessions deconnecte le compte de tous ses appareils")
    void revokingSessionsLogsEveryDeviceOut() {
        User target = storedUsers.get(TARGET_ID);
        tokenService.startSession(target, ClientType.MOBILE, DeviceContext.unknown());
        tokenService.startSession(target, ClientType.WEB, DeviceContext.unknown());

        adminUsers.revokeSessions(TARGET_ID);

        assertThat(adminUsers.activeSessions(TARGET_ID)).isEmpty();
    }

    private User userWithRoles(String id, String... roles) {
        User user = new User();
        user.setId(id);
        user.setEmail(id + "@example.com");
        user.setRoles(new java.util.LinkedHashSet<>(Set.of(roles)));
        return user;
    }
}
