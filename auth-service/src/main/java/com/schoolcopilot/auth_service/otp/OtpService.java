package com.schoolcopilot.auth_service.otp;

import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.schoolcopilot.auth_service.config.AuthProperties;
import com.schoolcopilot.auth_service.domain.OtpChallenge;
import com.schoolcopilot.auth_service.exception.AuthException;
import com.schoolcopilot.auth_service.repository.OtpChallengeRepository;
import com.schoolcopilot.auth_service.security.SecureTokens;

/**
 * Connexion par code SMS.
 *
 * <p>Trois garde-fous, sans alourdir l'experience : un delai entre deux envois
 * pour eviter le harcelement par SMS (et la facture qui va avec), un nombre
 * d'essais limite par code, et une duree de vie courte. Le code n'est jamais
 * stocke en clair.
 */
@Service
public class OtpService {

    private final OtpChallengeRepository repository;
    private final SmsSender smsSender;
    private final AuthProperties properties;

    public OtpService(OtpChallengeRepository repository, SmsSender smsSender,
            AuthProperties properties) {
        this.repository = repository;
        this.smsSender = smsSender;
        this.properties = properties;
    }

    /**
     * Un defi en attente. {@code devCode} n'est renseigne que si
     * {@code ojino.auth.otp.expose-code} est actif, pour pouvoir tester sans
     * fournisseur SMS. Il doit imperativement rester a false en production.
     */
    public record Challenge(String challengeId, Instant expiresAt, String devCode) {
    }

    public Challenge requestCode(String rawPhone) {
        String phone = PhoneNumbers.normalize(rawPhone);
        Instant now = Instant.now();

        enforceResendCooldown(phone, now);

        AuthProperties.Otp config = properties.otp();
        String code = SecureTokens.randomNumericCode(config.length());

        OtpChallenge challenge = new OtpChallenge();
        challenge.setPhone(phone);
        challenge.setCodeHash(SecureTokens.sha256(code));
        challenge.setCreatedAt(now);
        challenge.setExpiresAt(now.plus(config.ttl()));
        OtpChallenge saved = repository.save(challenge);

        smsSender.sendVerificationCode(phone, code);

        return new Challenge(saved.getId(), saved.getExpiresAt(),
                config.exposeCode() ? code : null);
    }

    /**
     * Valide le code et renvoie le numero normalise.
     *
     * @throws AuthException si le code est faux, expire, deja utilise, ou si le
     *         nombre d'essais est depasse
     */
    public String verifyCode(String challengeId, String rawPhone, String code) {
        String phone = PhoneNumbers.normalize(rawPhone);
        Instant now = Instant.now();

        OtpChallenge challenge = repository.findById(challengeId)
                .orElseThrow(AuthException::otpInvalid);

        if (!challenge.getPhone().equals(phone) || challenge.isConsumed()
                || challenge.isExpired(now)) {
            throw AuthException.otpInvalid();
        }

        if (challenge.getAttempts() >= properties.otp().maxAttempts()) {
            throw AuthException.otpTooManyAttempts();
        }

        if (!SecureTokens.matches(code, challenge.getCodeHash())) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            repository.save(challenge);
            throw AuthException.otpInvalid();
        }

        // Consomme immediatement : un code ne sert qu'une fois.
        challenge.setConsumedAt(now);
        repository.save(challenge);

        return phone;
    }

    private void enforceResendCooldown(String phone, Instant now) {
        repository.findFirstByPhoneOrderByCreatedAtDesc(phone).ifPresent(last -> {
            if (last.getCreatedAt() == null) {
                return;
            }
            Duration cooldown = properties.otp().resendCooldown();
            Instant nextAllowed = last.getCreatedAt().plus(cooldown);
            if (now.isBefore(nextAllowed)) {
                throw AuthException.otpThrottled(Duration.between(now, nextAllowed).toSeconds() + 1);
            }
        });
    }

    /** Garde-fou de demarrage sur une configuration incoherente. */
    void validateConfiguration() {
        if (properties.otp().length() < 4 || properties.otp().length() > 10) {
            throw new AuthException(HttpStatus.INTERNAL_SERVER_ERROR, "invalid_otp_config",
                    "ojino.auth.otp.length doit etre compris entre 4 et 10.");
        }
    }
}
