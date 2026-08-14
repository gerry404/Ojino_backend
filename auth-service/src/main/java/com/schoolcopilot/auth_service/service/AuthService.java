package com.schoolcopilot.auth_service.service;

import org.springframework.stereotype.Service;

import com.schoolcopilot.auth_service.domain.AuthProvider;
import com.schoolcopilot.auth_service.domain.ClientType;
import com.schoolcopilot.auth_service.domain.RefreshToken;
import com.schoolcopilot.auth_service.domain.User;
import com.schoolcopilot.auth_service.otp.OtpService;
import com.schoolcopilot.auth_service.security.JwtService;
import com.schoolcopilot.auth_service.security.TokenService;
import com.schoolcopilot.auth_service.security.TokenService.DeviceContext;
import com.schoolcopilot.auth_service.social.SocialUser;
import com.schoolcopilot.auth_service.social.SocialVerifiers;

/**
 * Chef d'orchestre : quelle que soit la maniere dont l'utilisateur prouve son
 * identite, tout se termine ici par la meme chose, l'ouverture d'une session.
 */
@Service
public class AuthService {

    private final AccountService accounts;
    private final OtpService otp;
    private final SocialVerifiers socialVerifiers;
    private final JwtService jwtService;
    private final TokenService tokenService;

    public AuthService(AccountService accounts, OtpService otp, SocialVerifiers socialVerifiers,
            JwtService jwtService, TokenService tokenService) {
        this.accounts = accounts;
        this.otp = otp;
        this.socialVerifiers = socialVerifiers;
        this.jwtService = jwtService;
        this.tokenService = tokenService;
    }

    /**
     * Une session fraichement ouverte.
     *
     * <p>{@code newAccount} indique au client qu'il doit enchainer sur le parcours
     * de creation de profil plutot que d'aller directement a l'accueil.
     */
    public record AuthResult(
            User user,
            boolean newAccount,
            JwtService.AccessToken accessToken,
            TokenService.IssuedRefreshToken refreshToken,
            ClientType clientType) {
    }

    public AuthResult register(String email, String password, String displayName,
            ClientType clientType, DeviceContext device) {
        AccountService.ResolvedAccount account =
                accounts.registerWithEmail(email, password, displayName);
        return openSession(account.user(), account.created(), clientType, device);
    }

    public AuthResult loginWithEmail(String email, String password, ClientType clientType,
            DeviceContext device) {
        User user = accounts.authenticateWithEmail(email, password);
        return openSession(user, false, clientType, device);
    }

    public OtpService.Challenge startPhoneLogin(String phone) {
        return otp.requestCode(phone);
    }

    /**
     * Il n'y a pas d'inscription separee par SMS : si le numero est inconnu, le
     * compte se cree a la volee. C'est exactement le parcours WhatsApp.
     */
    public AuthResult verifyPhoneLogin(String challengeId, String phone, String code,
            ClientType clientType, DeviceContext device) {
        String verifiedPhone = otp.verifyCode(challengeId, phone, code);
        AccountService.ResolvedAccount account = accounts.resolveByPhone(verifiedPhone);
        return openSession(account.user(), account.created(), clientType, device);
    }

    /** Meme endpoint pour Google et Apple : seul le provider change. */
    public AuthResult socialLogin(AuthProvider provider, String idToken, String displayNameHint,
            ClientType clientType, DeviceContext device) {
        SocialUser social = socialVerifiers.forProvider(provider).verify(idToken);
        AccountService.ResolvedAccount account = accounts.resolveBySocial(social, displayNameHint);
        return openSession(account.user(), account.created(), clientType, device);
    }

    /**
     * Le rafraichissement silencieux. C'est l'appel le plus frequent du service :
     * l'application le declenche toute seule quand l'access token approche de sa
     * fin, sans que l'utilisateur voie quoi que ce soit.
     */
    public AuthResult refresh(String rawRefreshToken, DeviceContext device) {
        TokenService.IssuedRefreshToken rotated = tokenService.rotate(rawRefreshToken, device);
        RefreshToken stored = rotated.stored();
        User user = accounts.requireById(stored.getUserId());
        return new AuthResult(user, false, jwtService.issue(user), rotated, stored.getClientType());
    }

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            tokenService.revoke(rawRefreshToken);
        }
    }

    public void logoutEverywhere(String userId) {
        tokenService.revokeAllForUser(userId);
    }

    public User currentUser(String userId) {
        return accounts.requireById(userId);
    }

    private AuthResult openSession(User user, boolean newAccount, ClientType clientType,
            DeviceContext device) {
        User touched = accounts.touchLastLogin(user);
        return new AuthResult(
                touched,
                newAccount,
                jwtService.issue(touched),
                tokenService.startSession(touched, clientType, device),
                clientType);
    }
}
