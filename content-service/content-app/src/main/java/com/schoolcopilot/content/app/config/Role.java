package com.schoolcopilot.content.app.config;

/**
 * Les roles portes par le claim {@code roles} de l'access token.
 *
 * <p>Volontairement redeclares ici plutot que partages avec l'auth-service : deux
 * microservices qui dependent d'une bibliotheque commune finissent par ne plus
 * pouvoir etre deployes separement. Deux constantes dupliquees coutent moins cher
 * que ce couplage.
 */
public final class Role {

    public static final String USER = "USER";

    public static final String ADMIN = "ADMIN";

    private Role() {
    }
}
