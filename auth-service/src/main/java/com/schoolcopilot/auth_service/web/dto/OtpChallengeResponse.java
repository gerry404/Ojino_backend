package com.schoolcopilot.auth_service.web.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Reponse a une demande de code SMS.
 *
 * <p>{@code devCode} n'apparait qu'en developpement, quand
 * {@code ojino.auth.otp.expose-code} est actif.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OtpChallengeResponse(String challengeId, Instant expiresAt, String devCode) {
}
