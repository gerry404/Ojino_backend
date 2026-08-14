package com.schoolcopilot.auth_service.domain;

/**
 * Les roles, stockes en clair sur le compte et recopies dans le claim
 * {@code roles} de l'access token.
 *
 * <p>Spring Security prefixe automatiquement par {@code ROLE_} : le role
 * {@code ADMIN} devient l'autorite {@code ROLE_ADMIN}. Les constantes ci-dessous
 * sont donc sans prefixe, c'est la forme stockee.
 */
public final class Role {

    /** Tout le monde. Attribue a la creation du compte. */
    public static final String USER = "USER";

    /** Acces au back-office, sous {@code /api/v1/admin}. */
    public static final String ADMIN = "ADMIN";

    /** Les seuls roles acceptes. Sert a rejeter les valeurs inventees. */
    public static final java.util.Set<String> KNOWN = java.util.Set.of(USER, ADMIN);

    private Role() {
    }
}
