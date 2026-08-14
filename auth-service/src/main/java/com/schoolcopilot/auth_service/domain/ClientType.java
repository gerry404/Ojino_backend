package com.schoolcopilot.auth_service.domain;

/**
 * D'ou vient la requete. Cela determine la duree du refresh token et la facon
 * dont il est transporte : dans le corps de la reponse pour le mobile (qui le
 * range dans son keystore), dans un cookie httpOnly pour le web.
 */
public enum ClientType {

    MOBILE,
    WEB;

    /** Lit l'en-tete {@code X-Client-Type}. Le mobile est le defaut. */
    public static ClientType from(String raw) {
        if (raw == null || raw.isBlank()) {
            return MOBILE;
        }
        return "web".equalsIgnoreCase(raw.trim()) ? WEB : MOBILE;
    }
}
