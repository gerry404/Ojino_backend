package com.schoolcopilot.auth_service.security;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.schoolcopilot.auth_service.config.AuthProperties;
import com.schoolcopilot.auth_service.domain.ClientType;
import com.schoolcopilot.auth_service.domain.RefreshToken;
import com.schoolcopilot.auth_service.domain.User;
import com.schoolcopilot.auth_service.exception.AuthException;
import com.schoolcopilot.auth_service.repository.RefreshTokenRepository;

/**
 * Cycle de vie des refresh tokens.
 *
 * <p>C'est ce service qui realise la promesse "on ne se reconnecte jamais" :
 * chaque rafraichissement consomme le token presente et en emet un nouveau avec
 * une duree de vie pleine. Tant que l'utilisateur ouvre l'application avant la
 * fin de la fenetre (un an sur mobile, trois mois sur le web), sa session ne
 * s'interrompt pas.
 *
 * <p>La rotation apporte aussi la detection de vol : un refresh token ne sert
 * qu'une fois. Si un token deja consomme revient, c'est qu'une copie circule, et
 * toute la famille est revoquee.
 */
@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private final RefreshTokenRepository repository;
    private final AuthProperties properties;

    public TokenService(RefreshTokenRepository repository, AuthProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /** Le token brut (a renvoyer au client, jamais reconstituable ensuite) et sa trace en base. */
    public record IssuedRefreshToken(String rawValue, RefreshToken stored) {
    }

    /** Ce que l'on sait de l'appareil, utile pour lister ou revoquer les sessions. */
    public record DeviceContext(String userAgent, String ipAddress) {

        public static DeviceContext unknown() {
            return new DeviceContext(null, null);
        }
    }

    public Duration ttlFor(ClientType clientType) {
        return clientType == ClientType.WEB
                ? properties.refresh().webTtl()
                : properties.refresh().mobileTtl();
    }

    /** Ouvre une nouvelle session : appele apres une connexion reussie. */
    public IssuedRefreshToken startSession(User user, ClientType clientType, DeviceContext device) {
        return create(user.getId(), UUID.randomUUID().toString(), clientType, device);
    }

    /**
     * Verifie le token presente puis le remplace.
     *
     * @throws AuthException si le token est inconnu, expire, revoque, ou deja consomme
     */
    public IssuedRefreshToken rotate(String rawToken, DeviceContext device) {
        RefreshToken current = repository.findByTokenHash(SecureTokens.sha256(rawToken))
                .orElseThrow(AuthException::invalidRefreshToken);

        Instant now = Instant.now();

        if (current.isRotated()) {
            // Ce token a deja servi. Soit un voleur l'utilise, soit le vrai
            // utilisateur rejoue une requete apres avoir perdu la reponse. Dans le
            // doute on coupe la session entiere : c'est le seul moment ou l'on
            // demande une reconnexion.
            log.warn("Refresh token deja consomme reutilise (famille {}, utilisateur {}) : "
                    + "revocation de toute la famille.", current.getFamilyId(), current.getUserId());
            revokeFamily(current.getFamilyId(), now);
            throw AuthException.invalidRefreshToken();
        }

        if (!current.isUsable(now)) {
            throw AuthException.invalidRefreshToken();
        }

        IssuedRefreshToken next = create(current.getUserId(), current.getFamilyId(),
                current.getClientType(), device);

        current.setRotatedAt(now);
        current.setReplacedById(next.stored().getId());
        repository.save(current);

        return next;
    }

    /** Retrouve le token en verifiant qu'il est bien utilisable, sans le consommer. */
    public RefreshToken requireUsable(String rawToken) {
        RefreshToken token = repository.findByTokenHash(SecureTokens.sha256(rawToken))
                .orElseThrow(AuthException::invalidRefreshToken);
        if (!token.isUsable(Instant.now())) {
            throw AuthException.invalidRefreshToken();
        }
        return token;
    }

    /** Deconnexion d'un seul appareil. Silencieuse si le token est deja inconnu. */
    public void revoke(String rawToken) {
        repository.findByTokenHash(SecureTokens.sha256(rawToken)).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            repository.save(token);
        });
    }

    /** Deconnexion de tous les appareils : changement de mot de passe, appareil perdu. */
    public void revokeAllForUser(String userId) {
        Instant now = Instant.now();
        List<RefreshToken> tokens = repository.findByUserId(userId);
        tokens.forEach(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(now);
            }
        });
        repository.saveAll(tokens);
    }

    private void revokeFamily(String familyId, Instant now) {
        List<RefreshToken> family = repository.findByFamilyId(familyId);
        family.forEach(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(now);
            }
        });
        repository.saveAll(family);
    }

    private IssuedRefreshToken create(String userId, String familyId, ClientType clientType,
            DeviceContext device) {
        Instant now = Instant.now();
        String rawValue = SecureTokens.randomToken();

        RefreshToken token = new RefreshToken();
        token.setTokenHash(SecureTokens.sha256(rawValue));
        token.setUserId(userId);
        token.setFamilyId(familyId);
        token.setClientType(clientType);
        token.setIssuedAt(now);
        // Duree pleine a chaque rotation : c'est ce "glissement" qui evite les
        // reconnexions pour un utilisateur regulier.
        token.setExpiresAt(now.plus(ttlFor(clientType)));
        token.setUserAgent(device.userAgent());
        token.setIpAddress(device.ipAddress());

        return new IssuedRefreshToken(rawValue, repository.save(token));
    }
}
