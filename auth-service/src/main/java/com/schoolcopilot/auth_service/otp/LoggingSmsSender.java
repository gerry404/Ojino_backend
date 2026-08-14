package com.schoolcopilot.auth_service.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation de developpement : le code part dans les logs au lieu d'un vrai
 * SMS. Elle permet de tester tout le parcours sans fournisseur ni budget.
 *
 * <p>Elle s'efface d'elle-meme des qu'un autre {@link SmsSender} est declare
 * (voir {@code OtpConfig}).
 */
public class LoggingSmsSender implements SmsSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingSmsSender.class);

    @Override
    public void sendVerificationCode(String phone, String code) {
        log.warn("[SMS SIMULE] Code de verification pour {} : {}", phone, code);
    }
}
