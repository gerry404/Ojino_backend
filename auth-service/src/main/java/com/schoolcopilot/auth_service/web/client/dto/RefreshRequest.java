package com.schoolcopilot.auth_service.web.client.dto;

/**
 * Corps facultatif du rafraichissement.
 *
 * <p>Le mobile y place son refresh token. Le web n'envoie rien : son token voyage
 * dans le cookie httpOnly, hors de portee du JavaScript de la page.
 */
public record RefreshRequest(String refreshToken) {
}
