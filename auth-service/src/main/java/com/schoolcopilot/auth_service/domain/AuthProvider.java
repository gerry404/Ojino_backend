package com.schoolcopilot.auth_service.domain;

/** Les quatre portes d'entree vers un compte. Toutes menent au meme {@link User}. */
public enum AuthProvider {

    /** Email + mot de passe. */
    EMAIL,

    /** Numero de telephone verifie par code SMS, sans mot de passe. */
    PHONE,

    /** Google Sign-In (ID token verifie via le JWKS de Google). */
    GOOGLE,

    /** Sign in with Apple (identity token verifie via le JWKS d'Apple). */
    APPLE
}
