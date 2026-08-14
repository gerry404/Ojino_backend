package com.schoolcopilot.auth_service.otp;

/**
 * Envoi du SMS contenant le code.
 *
 * <p>Abstrait volontairement : le jour ou un compte Twilio, Vonage ou un agregateur
 * local est ouvert, il suffit de declarer un bean qui implemente cette interface
 * pour remplacer l'implementation de developpement, sans toucher au reste.
 */
public interface SmsSender {

    void sendVerificationCode(String phone, String code);
}
