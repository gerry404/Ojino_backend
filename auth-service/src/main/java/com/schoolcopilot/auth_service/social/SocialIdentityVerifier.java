package com.schoolcopilot.auth_service.social;

import com.schoolcopilot.auth_service.domain.AuthProvider;

/**
 * Verifie l'ID token qu'une application cliente a obtenu aupres d'un fournisseur.
 *
 * <p>Le flux est celui des applications natives : le SDK Google ou Apple s'occupe
 * de l'ecran de connexion cote client et nous renvoie un token signe. Le serveur
 * ne fait que verifier cette signature, il ne manipule jamais les mots de passe
 * Google ou Apple de l'utilisateur.
 */
public interface SocialIdentityVerifier {

    AuthProvider provider();

    /**
     * @throws com.schoolcopilot.auth_service.exception.AuthException si le token est
     *         invalide, expire, destine a une autre application, ou si le provider
     *         n'est pas encore configure sur ce serveur
     */
    SocialUser verify(String idToken);
}
