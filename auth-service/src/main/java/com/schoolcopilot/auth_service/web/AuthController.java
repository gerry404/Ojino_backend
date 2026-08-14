package com.schoolcopilot.auth_service.web;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.schoolcopilot.auth_service.domain.ClientType;
import com.schoolcopilot.auth_service.exception.AuthException;
import com.schoolcopilot.auth_service.otp.OtpService;
import com.schoolcopilot.auth_service.security.TokenService;
import com.schoolcopilot.auth_service.security.TokenService.DeviceContext;
import com.schoolcopilot.auth_service.service.AuthService;
import com.schoolcopilot.auth_service.web.dto.AuthResponse;
import com.schoolcopilot.auth_service.web.dto.LoginRequest;
import com.schoolcopilot.auth_service.web.dto.OtpChallengeResponse;
import com.schoolcopilot.auth_service.web.dto.PhoneStartRequest;
import com.schoolcopilot.auth_service.web.dto.PhoneVerifyRequest;
import com.schoolcopilot.auth_service.web.dto.RefreshRequest;
import com.schoolcopilot.auth_service.web.dto.RegisterRequest;
import com.schoolcopilot.auth_service.web.dto.SocialLoginRequest;
import com.schoolcopilot.auth_service.web.dto.UserResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * L'API d'authentification.
 *
 * <p>Toutes les routes acceptent l'en-tete {@code X-Client-Type} valant
 * {@code mobile} (defaut) ou {@code web}. C'est lui qui decide de la duree de la
 * session et du transport du refresh token.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;
    private final RefreshCookies refreshCookies;

    public AuthController(AuthService authService, TokenService tokenService,
            RefreshCookies refreshCookies) {
        this.authService = authService;
        this.tokenService = tokenService;
        this.refreshCookies = refreshCookies;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader(value = "X-Client-Type", required = false) String clientTypeHeader,
            HttpServletRequest httpRequest) {

        AuthService.AuthResult result = authService.register(
                request.email(), request.password(), request.displayName(),
                ClientType.from(clientTypeHeader), deviceOf(httpRequest));

        return respond(result, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Client-Type", required = false) String clientTypeHeader,
            HttpServletRequest httpRequest) {

        AuthService.AuthResult result = authService.loginWithEmail(
                request.email(), request.password(),
                ClientType.from(clientTypeHeader), deviceOf(httpRequest));

        return respond(result, HttpStatus.OK);
    }

    /** Envoie un code SMS. Ne dit jamais si le numero possede deja un compte. */
    @PostMapping("/phone/start")
    public OtpChallengeResponse startPhoneLogin(@Valid @RequestBody PhoneStartRequest request) {
        OtpService.Challenge challenge = authService.startPhoneLogin(request.phone());
        return new OtpChallengeResponse(challenge.challengeId(), challenge.expiresAt(),
                challenge.devCode());
    }

    /** Valide le code. Cree le compte au passage s'il n'existait pas. */
    @PostMapping("/phone/verify")
    public ResponseEntity<AuthResponse> verifyPhoneLogin(
            @Valid @RequestBody PhoneVerifyRequest request,
            @RequestHeader(value = "X-Client-Type", required = false) String clientTypeHeader,
            HttpServletRequest httpRequest) {

        AuthService.AuthResult result = authService.verifyPhoneLogin(
                request.challengeId(), request.phone(), request.code(),
                ClientType.from(clientTypeHeader), deviceOf(httpRequest));

        return respond(result, result.newAccount() ? HttpStatus.CREATED : HttpStatus.OK);
    }

    /** Connexion Google ou Apple, selon le champ {@code provider}. */
    @PostMapping("/social")
    public ResponseEntity<AuthResponse> socialLogin(
            @Valid @RequestBody SocialLoginRequest request,
            @RequestHeader(value = "X-Client-Type", required = false) String clientTypeHeader,
            HttpServletRequest httpRequest) {

        AuthService.AuthResult result = authService.socialLogin(
                request.provider(), request.idToken(), request.displayName(),
                ClientType.from(clientTypeHeader), deviceOf(httpRequest));

        return respond(result, result.newAccount() ? HttpStatus.CREATED : HttpStatus.OK);
    }

    /**
     * Rafraichissement silencieux : l'application l'appelle d'elle-meme quand
     * l'access token arrive a expiration. L'utilisateur ne voit rien.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody(required = false) RefreshRequest request,
            HttpServletRequest httpRequest) {

        String rawToken = extractRefreshToken(request, httpRequest);
        AuthService.AuthResult result = authService.refresh(rawToken, deviceOf(httpRequest));
        return respond(result, HttpStatus.OK);
    }

    /** Deconnexion de cet appareil uniquement. */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestBody(required = false) RefreshRequest request,
            HttpServletRequest httpRequest) {

        refreshCookies.read(httpRequest)
                .or(() -> java.util.Optional.ofNullable(
                        request == null ? null : request.refreshToken()))
                .ifPresent(authService::logout);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookies.clear().toString())
                .body(Map.of("status", "logged_out"));
    }

    /** Deconnexion de tous les appareils. Demande un access token valide. */
    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutEverywhere(@AuthenticationPrincipal Jwt jwt) {
        authService.logoutEverywhere(jwt.getSubject());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookies.clear().toString())
                .body(Map.of("status", "logged_out_everywhere"));
    }

    /** Le compte associe a l'access token presente. */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return UserResponse.from(authService.currentUser(jwt.getSubject()));
    }

    // ------------------------------------------------------------------

    /**
     * Le cookie prime sur le corps de la requete : si les deux sont presents,
     * c'est un navigateur, et le cookie est la source la plus fiable.
     */
    private String extractRefreshToken(RefreshRequest request, HttpServletRequest httpRequest) {
        return refreshCookies.read(httpRequest)
                .or(() -> java.util.Optional.ofNullable(
                        request == null ? null : request.refreshToken()))
                .filter(token -> !token.isBlank())
                .orElseThrow(AuthException::invalidRefreshToken);
    }

    private ResponseEntity<AuthResponse> respond(AuthService.AuthResult result, HttpStatus status) {
        boolean isWeb = result.clientType() == ClientType.WEB;

        AuthResponse body = AuthResponse.of(
                result.accessToken().value(),
                result.accessToken().expiresInSeconds(),
                isWeb ? null : result.refreshToken().rawValue(),
                result.newAccount(),
                UserResponse.from(result.user()));

        ResponseEntity.BodyBuilder response = ResponseEntity.status(status);
        if (isWeb) {
            ResponseCookie cookie = refreshCookies.issue(
                    result.refreshToken().rawValue(),
                    tokenService.ttlFor(ClientType.WEB));
            response.header(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return response.body(body);
    }

    private DeviceContext deviceOf(HttpServletRequest request) {
        return new DeviceContext(request.getHeader(HttpHeaders.USER_AGENT), clientIp(request));
    }

    /** Derriere un reverse proxy, l'IP reelle est dans {@code X-Forwarded-For}. */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
