package com.schoolcopilot.auth_service.otp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.schoolcopilot.auth_service.TestFixtures;
import com.schoolcopilot.auth_service.domain.OtpChallenge;
import com.schoolcopilot.auth_service.exception.AuthException;
import com.schoolcopilot.auth_service.repository.OtpChallengeRepository;

class OtpServiceTest {

    private static final String PHONE = "+237690000000";

    private final Map<String, OtpChallenge> stored = new LinkedHashMap<>();
    private OtpChallengeRepository repository;
    private OtpService otpService;
    private String lastSentCode;

    @BeforeEach
    void setUp() {
        stored.clear();
        lastSentCode = null;
        repository = mock(OtpChallengeRepository.class);

        when(repository.save(any(OtpChallenge.class))).thenAnswer(invocation -> {
            OtpChallenge challenge = invocation.getArgument(0);
            if (challenge.getId() == null) {
                challenge.setId(UUID.randomUUID().toString());
            }
            stored.put(challenge.getId(), challenge);
            return challenge;
        });
        when(repository.findById(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(stored.get(invocation.getArgument(0))));
        when(repository.findFirstByPhoneOrderByCreatedAtDesc(anyString()))
                .thenAnswer(invocation -> stored.values().stream()
                        .filter(challenge -> challenge.getPhone().equals(invocation.getArgument(0)))
                        .reduce((first, second) -> second));

        SmsSender sender = (phone, code) -> lastSentCode = code;
        otpService = new OtpService(repository, sender, TestFixtures.properties());
    }

    @Test
    @DisplayName("le code part par SMS et n'est stocke que sous forme d'empreinte")
    void codeIsSentAndOnlyHashedIsStored() {
        OtpService.Challenge challenge = otpService.requestCode(PHONE);

        assertThat(lastSentCode).hasSize(6).containsOnlyDigits();
        assertThat(stored.get(challenge.challengeId()).getCodeHash()).isNotEqualTo(lastSentCode);
    }

    @Test
    @DisplayName("le bon code valide le defi et renvoie le numero normalise")
    void correctCodeVerifies() {
        OtpService.Challenge challenge = otpService.requestCode("+237 690 00 00 00");

        String phone = otpService.verifyCode(challenge.challengeId(), PHONE, lastSentCode);

        assertThat(phone).isEqualTo(PHONE);
    }

    @Test
    @DisplayName("un code ne sert qu'une fois")
    void codeCannotBeReplayed() {
        OtpService.Challenge challenge = otpService.requestCode(PHONE);
        otpService.verifyCode(challenge.challengeId(), PHONE, lastSentCode);

        assertThatThrownBy(() -> otpService.verifyCode(challenge.challengeId(), PHONE, lastSentCode))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("code", "otp_invalid");
    }

    @Test
    @DisplayName("un mauvais code est refuse et consomme un essai")
    void wrongCodeCountsAsAnAttempt() {
        OtpService.Challenge challenge = otpService.requestCode(PHONE);

        assertThatThrownBy(() -> otpService.verifyCode(challenge.challengeId(), PHONE, "000000"))
                .isInstanceOf(AuthException.class);

        assertThat(stored.get(challenge.challengeId()).getAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("au dela de cinq essais le defi est bloque, meme avec le bon code")
    void bruteForceIsStopped() {
        OtpService.Challenge challenge = otpService.requestCode(PHONE);
        String goodCode = lastSentCode;

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> otpService.verifyCode(challenge.challengeId(), PHONE, "000000"))
                    .isInstanceOf(AuthException.class);
        }

        assertThatThrownBy(() -> otpService.verifyCode(challenge.challengeId(), PHONE, goodCode))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("code", "otp_too_many_attempts");
    }

    @Test
    @DisplayName("un code expire est refuse")
    void expiredCodeIsRejected() {
        OtpService.Challenge challenge = otpService.requestCode(PHONE);
        stored.get(challenge.challengeId()).setExpiresAt(Instant.now().minusSeconds(1));

        assertThatThrownBy(() -> otpService.verifyCode(challenge.challengeId(), PHONE, lastSentCode))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("deux demandes coup sur coup sont bloquees par le delai anti-renvoi")
    void resendIsThrottled() {
        otpService.requestCode(PHONE);

        assertThatThrownBy(() -> otpService.requestCode(PHONE))
                .isInstanceOf(AuthException.class)
                .hasFieldOrPropertyWithValue("code", "otp_throttled");
    }

    @Test
    @DisplayName("le code d'un numero ne vaut pas pour un autre")
    void challengeIsBoundToItsPhoneNumber() {
        OtpService.Challenge challenge = otpService.requestCode(PHONE);

        assertThatThrownBy(() ->
                otpService.verifyCode(challenge.challengeId(), "+237691111111", lastSentCode))
                .isInstanceOf(AuthException.class);
    }
}
