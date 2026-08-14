package com.schoolcopilot.auth_service.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Un refresh token stocke cote serveur.
 *
 * <p>Seule l'empreinte SHA-256 est conservee : une fuite de la base ne permet donc
 * pas d'usurper une session. Le token brut n'existe que chez le client.
 *
 * <p>Chaque rotation cree un nouveau document rattache a la meme {@code familyId}.
 * Si un token deja tourne est represente, c'est qu'il a ete vole : on revoque
 * alors toute la famille (voir {@code TokenService}).
 */
@Document(collection = "refresh_tokens")
public class RefreshToken {

    @Id
    private String id;

    @Indexed(unique = true)
    private String tokenHash;

    @Indexed
    private String userId;

    /** Relie toutes les rotations successives d'une meme session. */
    @Indexed
    private String familyId;

    private ClientType clientType;

    private Instant issuedAt;

    /**
     * Mongo supprime le document de lui-meme a cette date : les sessions expirees
     * ne s'accumulent pas.
     */
    @Indexed(expireAfter = "0s")
    private Instant expiresAt;

    /** Renseigne quand le token a servi et a ete remplace. */
    private Instant rotatedAt;

    private Instant revokedAt;

    private String replacedById;

    private String userAgent;

    private String ipAddress;

    public boolean isExpired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    public boolean isRotated() {
        return rotatedAt != null;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    /** Un token n'est utilisable qu'une fois : ni tourne, ni revoque, ni expire. */
    public boolean isUsable(Instant now) {
        return !isRotated() && !isRevoked() && !isExpired(now);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFamilyId() {
        return familyId;
    }

    public void setFamilyId(String familyId) {
        this.familyId = familyId;
    }

    public ClientType getClientType() {
        return clientType;
    }

    public void setClientType(ClientType clientType) {
        this.clientType = clientType;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRotatedAt() {
        return rotatedAt;
    }

    public void setRotatedAt(Instant rotatedAt) {
        this.rotatedAt = rotatedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getReplacedById() {
        return replacedById;
    }

    public void setReplacedById(String replacedById) {
        this.replacedById = replacedById;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
}
