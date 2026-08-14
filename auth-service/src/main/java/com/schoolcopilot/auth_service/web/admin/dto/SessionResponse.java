package com.schoolcopilot.auth_service.web.admin.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.schoolcopilot.auth_service.domain.ClientType;
import com.schoolcopilot.auth_service.domain.RefreshToken;

/**
 * Une session ouverte, telle que le back-office la voit.
 *
 * <p>Ni le token ni son empreinte n'apparaissent : afficher l'empreinte
 * n'apporterait rien et exposerait inutilement le contenu de la base.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SessionResponse(
        String id,
        ClientType clientType,
        String userAgent,
        String ipAddress,
        Instant issuedAt,
        Instant expiresAt) {

    public static SessionResponse from(RefreshToken token) {
        return new SessionResponse(
                token.getId(),
                token.getClientType(),
                token.getUserAgent(),
                token.getIpAddress(),
                token.getIssuedAt(),
                token.getExpiresAt());
    }
}
