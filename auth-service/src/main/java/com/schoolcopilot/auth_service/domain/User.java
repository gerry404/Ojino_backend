package com.schoolcopilot.auth_service.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Un compte utilisateur.
 *
 * <p>{@code passwordHash}, {@code email} et {@code phone} sont tous facultatifs :
 * quelqu'un qui s'inscrit par Apple avec l'option "masquer mon adresse" n'a pas de
 * mot de passe et pas de numero, et c'est parfaitement valide. La seule chose
 * garantie est qu'un compte possede au moins une {@link LinkedIdentity}.
 */
@Document(collection = "users")
@CompoundIndex(name = "idx_user_identity",
        def = "{'identities.provider': 1, 'identities.subject': 1}",
        unique = true, sparse = true)
public class User {

    @Id
    private String id;

    @Indexed(unique = true, sparse = true)
    private String email;

    private boolean emailVerified;

    /** Au format E.164, par exemple {@code +237690000000}. */
    @Indexed(unique = true, sparse = true)
    private String phone;

    private boolean phoneVerified;

    /** BCrypt. Null pour les comptes crees par SMS ou par un provider social. */
    private String passwordHash;

    private String displayName;

    private String avatarUrl;

    private Set<String> roles = new LinkedHashSet<>();

    private List<LinkedIdentity> identities = new ArrayList<>();

    private boolean disabled;

    private Instant createdAt;

    private Instant updatedAt;

    private Instant lastLoginAt;

    /** Renvoie l'identite correspondant a ce provider, si le compte en possede une. */
    public Optional<LinkedIdentity> identityFor(AuthProvider provider) {
        return identities.stream().filter(it -> it.provider() == provider).findFirst();
    }

    public boolean hasIdentityFor(AuthProvider provider) {
        return identityFor(provider).isPresent();
    }

    /** Rattache une nouvelle facon de se connecter, sans jamais creer de doublon. */
    public void linkIdentity(AuthProvider provider, String subject) {
        boolean alreadyLinked = identities.stream()
                .anyMatch(it -> it.provider() == provider && it.subject().equals(subject));
        if (!alreadyLinked) {
            identities.add(LinkedIdentity.of(provider, subject));
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isPhoneVerified() {
        return phoneVerified;
    }

    public void setPhoneVerified(boolean phoneVerified) {
        this.phoneVerified = phoneVerified;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public List<LinkedIdentity> getIdentities() {
        return identities;
    }

    public void setIdentities(List<LinkedIdentity> identities) {
        this.identities = identities;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
