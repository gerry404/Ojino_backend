package com.schoolcopilot.auth_service.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schoolcopilot.auth_service.TestFixtures;
import com.schoolcopilot.auth_service.domain.ClientType;
import com.schoolcopilot.auth_service.domain.RefreshToken;
import com.schoolcopilot.auth_service.domain.User;
import com.schoolcopilot.auth_service.exception.AuthException;
import com.schoolcopilot.auth_service.repository.RefreshTokenRepository;
import com.schoolcopilot.auth_service.security.TokenService.DeviceContext;
import com.schoolcopilot.auth_service.security.TokenService.IssuedRefreshToken;

class TokenServiceTest {

    private final Map<String, RefreshToken> stored = new LinkedHashMap<>();
    private RefreshTokenRepository repository;
    private TokenService tokenService;
    private User user;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        stored.clear();
        repository = mock(RefreshTokenRepository.class);

        when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            if (token.getId() == null) {
                token.setId(UUID.randomUUID().toString());
            }
            stored.put(token.getTokenHash(), token);
            return token;
        });
        when(repository.saveAll(any())).thenAnswer(invocation -> {
            List<RefreshToken> tokens = new ArrayList<>((Collection<RefreshToken>) invocation.getArgument(0));
            tokens.forEach(token -> stored.put(token.getTokenHash(), token));
            return tokens;
        });
        when(repository.findByTokenHash(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(stored.get(invocation.getArgument(0))));
        when(repository.findByFamilyId(anyString())).thenAnswer(invocation -> stored.values().stream()
                .filter(token -> invocation.getArgument(0).equals(token.getFamilyId()))
                .toList());
        when(repository.findByUserId(anyString())).thenAnswer(invocation -> stored.values().stream()
                .filter(token -> invocation.getArgument(0).equals(token.getUserId()))
                .toList());

        tokenService = new TokenService(repository, TestFixtures.properties());
        user = TestFixtures.user("user-1");
    }

    @Test
    @DisplayName("le mobile recoit une session d'un an, le web de trois mois")
    void sessionLengthDependsOnClient() {
        assertThat(tokenService.ttlFor(ClientType.MOBILE)).isEqualTo(Duration.ofDays(365));
        assertThat(tokenService.ttlFor(ClientType.WEB)).isEqualTo(Duration.ofDays(90));

        IssuedRefreshToken mobile = tokenService.startSession(user, ClientType.MOBILE,
                DeviceContext.unknown());

        long days = ChronoUnit.DAYS.between(Instant.now(), mobile.stored().getExpiresAt());
        assertThat(days).isBetween(364L, 365L);
    }

    @Test
    @DisplayName("le token brut n'est jamais stocke, seulement son empreinte")
    void onlyTheHashIsPersisted() {
        IssuedRefreshToken issued = tokenService.startSession(user, ClientType.MOBILE,
                DeviceContext.unknown());

        assertThat(issued.stored().getTokenHash())
                .isNotEqualTo(issued.rawValue())
                .isEqualTo(SecureTokens.sha256(issued.rawValue()));
    }

    @Test
    @DisplayName("la rotation consomme l'ancien token et en emet un nouveau dans la meme famille")
    void rotationReplacesTheToken() {
        IssuedRefreshToken first = tokenService.startSession(user, ClientType.MOBILE,
                DeviceContext.unknown());

        IssuedRefreshToken second = tokenService.rotate(first.rawValue(), DeviceContext.unknown());

        assertThat(second.rawValue()).isNotEqualTo(first.rawValue());
        assertThat(second.stored().getFamilyId()).isEqualTo(first.stored().getFamilyId());
        assertThat(first.stored().isRotated()).isTrue();
        assertThat(first.stored().getReplacedById()).isEqualTo(second.stored().getId());
    }

    @Test
    @DisplayName("la duree repart a zero a chaque rotation : c'est ce qui evite les reconnexions")
    void rotationSlidesTheExpiry() {
        IssuedRefreshToken first = tokenService.startSession(user, ClientType.MOBILE,
                DeviceContext.unknown());
        Instant firstExpiry = first.stored().getExpiresAt();

        IssuedRefreshToken second = tokenService.rotate(first.rawValue(), DeviceContext.unknown());

        assertThat(second.stored().getExpiresAt()).isAfterOrEqualTo(firstExpiry);
    }

    @Test
    @DisplayName("rejouer un token deja consomme revoque toute la famille")
    void reuseOfARotatedTokenKillsTheFamily() {
        IssuedRefreshToken first = tokenService.startSession(user, ClientType.MOBILE,
                DeviceContext.unknown());
        IssuedRefreshToken second = tokenService.rotate(first.rawValue(), DeviceContext.unknown());

        // Un voleur rejoue le premier token, deja consomme.
        assertThatThrownBy(() -> tokenService.rotate(first.rawValue(), DeviceContext.unknown()))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("code", "invalid_refresh_token");

        // Le token legitime du vrai utilisateur est neutralise lui aussi : dans le
        // doute, la session entiere tombe.
        assertThat(stored.get(second.stored().getTokenHash()).isRevoked()).isTrue();
    }

    @Test
    @DisplayName("un token expire est refuse")
    void expiredTokenIsRejected() {
        IssuedRefreshToken issued = tokenService.startSession(user, ClientType.MOBILE,
                DeviceContext.unknown());
        issued.stored().setExpiresAt(Instant.now().minusSeconds(1));

        assertThatThrownBy(() -> tokenService.rotate(issued.rawValue(), DeviceContext.unknown()))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("un token inconnu est refuse")
    void unknownTokenIsRejected() {
        assertThatThrownBy(() -> tokenService.rotate("token-invente", DeviceContext.unknown()))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("la deconnexion globale revoque toutes les sessions de l'utilisateur")
    void logoutEverywhereRevokesEverySession() {
        IssuedRefreshToken phone = tokenService.startSession(user, ClientType.MOBILE,
                DeviceContext.unknown());
        IssuedRefreshToken laptop = tokenService.startSession(user, ClientType.WEB,
                DeviceContext.unknown());

        tokenService.revokeAllForUser(user.getId());

        assertThat(stored.get(phone.stored().getTokenHash()).isRevoked()).isTrue();
        assertThat(stored.get(laptop.stored().getTokenHash()).isRevoked()).isTrue();
    }
}
